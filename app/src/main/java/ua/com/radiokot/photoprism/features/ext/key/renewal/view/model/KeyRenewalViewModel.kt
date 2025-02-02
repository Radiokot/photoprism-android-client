package ua.com.radiokot.photoprism.features.ext.key.renewal.view.model

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.ext.key.renewal.logic.RenewEnteredKeyUseCase
import java.util.concurrent.TimeUnit

class KeyRenewalViewModel(
    private val application: Application,
    private val renewEnteredKeyUseCase: RenewEnteredKeyUseCase,
) : ViewModel() {

    private val log = kLogger("KeyRenewalVM")
    private val stateSubject: BehaviorSubject<State> = BehaviorSubject.create()
    val state: Observable<State> = stateSubject.observeOnMain()
    private val eventSubject: PublishSubject<Event> = PublishSubject.create()
    val events: Observable<Event> = eventSubject.observeOnMain()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    private var isInitialized = false

    fun initOnce(
        key: String,
    ) {
        if (isInitialized) {
            return
        }

        stateSubject.onNext(
            State.Preparing(
                key = key,
            )
        )

        log.debug {
            "init(): initialized:" +
                    "\nkey=$key"
        }

        isInitialized = true
    }

    fun onContinueClicked() = when (val state = stateSubject.value!!) {
        is State.Done -> {
            log.debug {
                "onContinueClicked(): going_to_activation"
            }

            eventSubject.onNext(
                Event.GoToActivation(
                    key = state.newKey,
                )
            )
        }

        is State.Preparing -> {
            log.debug {
                "onContinueClicked(): renewing"
            }

            renew(
                key = state.key,
            )
        }
    }

    private var renewalDisposable: Disposable? = null
    private fun renew(key: String) {
        renewalDisposable?.dispose()
        renewalDisposable = Single.zip(
            renewEnteredKeyUseCase.invoke(key),
            // Delay for better visual.
            Single.timer(1, TimeUnit.SECONDS, Schedulers.io())
        ) { newKey, _ -> newKey }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                log.debug {
                    "renew(): starting_renewal:" +
                            "\nkey=$key"
                }

                isLoading.postValue(true)
            }
            .doOnTerminate {
                isLoading.postValue(false)
            }
            .subscribeBy(
                onSuccess = { newKey ->
                    log.debug {
                        "renew(): successfully_renewed:" +
                                "\nnewKey=$newKey"
                    }

                    stateSubject.onNext(
                        State.Done(
                            newKey = newKey,
                        )
                    )
                },
                onError = { error ->
                    when (error) {
                        is RenewEnteredKeyUseCase.RenewalNotAvailableException -> {
                            log.debug {
                                "renew(): renewal_not_available"
                            }

                            eventSubject.onNext(Event.ShowFloatingError(Error.NotAvailable))
                        }

                        else -> {
                            log.error(error) {
                                "renew(): renewal_failed"
                            }

                            eventSubject.onNext(Event.ShowFloatingError(Error.Failed))
                        }
                    }
                }
            )
            .autoDispose(this)
    }

    fun onCopyKeyClicked() {
        val doneState = stateSubject.value as? State.Done
            ?: error("Copy is only clickable in Done state")

        application.getSystemService<ClipboardManager>()
            ?.setPrimaryClip(
                ClipData.newPlainText("Key", doneState.newKey)
            )

        log.debug {
            "onCopyKeyClicked(): copied"
        }
    }

    sealed interface State {
        class Preparing(
            val key: String,
        ) : State

        class Done(
            val newKey: String,
        ) : State
    }

    sealed interface Event {
        class GoToActivation(
            val key: String,
        ) : Event

        class ShowFloatingError(
            val error: Error,
        ) : Event
    }

    sealed interface Error {
        /**
         * Renewal failed and could be retried.
         */
        object Failed : Error

        /**
         * Renewal is not available as already used recently.
         */
        object NotAvailable : Error
    }
}
