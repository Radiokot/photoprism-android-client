package ua.com.radiokot.photoprism.features.ext.key.renewal.view.model

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain

class KeyRenewalViewModel(
    private val application: Application,
) : ViewModel() {

    private val log = kLogger("KeyRenewalVM")
    private val stateSubject: BehaviorSubject<State> = BehaviorSubject.create()
    val state: Observable<State> = stateSubject.observeOnMain()
    private val eventSubject: PublishSubject<Event> = PublishSubject.create()
    val events: Observable<Event> = eventSubject.observeOnMain()

    fun init(
        key: String,
    ) {
        stateSubject.onNext(
            State.Preparing(
                key = key,
            )
        )

        log.debug {
            "init(): initialized:" +
                    "\nkey=$key"
        }
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

    private fun renew(key: String) {
        // TODO renew key
        stateSubject.onNext(
            State.Done(
                newKey = "new key $key new key",
            )
        )
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
    }
}
