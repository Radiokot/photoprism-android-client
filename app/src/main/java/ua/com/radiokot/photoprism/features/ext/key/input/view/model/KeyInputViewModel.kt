package ua.com.radiokot.photoprism.features.ext.key.input.view.model

import android.app.Application
import android.content.ClipboardManager
import androidx.core.content.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.key.input.logic.ParseEnteredKeyUseCase

class KeyInputViewModel(
    private val application: Application,
    private val parseEnteredKeyUseCaseFactory: ParseEnteredKeyUseCase.Factory,
) : ViewModel() {
    private val log = kLogger("KeyInputVM")

    val key = MutableLiveData<String>()
    val keyError = MutableLiveData<KeyError?>()
    val canSubmitKeyInput = MutableLiveData<Boolean>()

    private val stateSubject = BehaviorSubject.createDefault<State>(State.Entering)
    val state = stateSubject.toMainThreadObservable()
    val currentState: State
        get() = stateSubject.value!!
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()

    init {
        val updateSubmitInput = { _: Any? ->
            canSubmitKeyInput.postValue(
                !key.value.isNullOrBlank() && keyError.value == null
            )
        }

        key.observeForever {
            keyError.value = null
            updateSubmitInput(null)
        }
        keyError.observeForever(updateSubmitInput)

        updateSubmitInput(null)
    }

    fun onKeyInputPasteClicked() {
        check(currentState is State.Entering) {
            "Paste button can only be clicked in the entering state"
        }

        val clipboardText = application.getSystemService<ClipboardManager>()
            ?.primaryClip
            ?.getItemAt(0)
            ?.text
            ?.toString()

        if (clipboardText != null) {
            log.debug {
                "onKeyInputPasteClicked(): replacing_key_with_clipboard_text"
            }

            // Lint complains on LiveData nullability otherwise.
            clipboardText.also(key::setValue)

            parseEnteredKey()
        } else {
            log.debug {
                "onKeyInputPasteClicked(): clipboard_is_empty"
            }
        }
    }

    fun onKeyInputSubmit() {
        check(
            currentState is State.Entering
                    && canSubmitKeyInput.value == true
        ) {
            "This input can only submitted in the entering state when it is allowed"
        }

        parseEnteredKey()
    }

    private fun parseEnteredKey() {
        val keyInput = key.value.checkNotNull {
            "The key must be entered at this point"
        }

        val useCase = parseEnteredKeyUseCaseFactory.get(
            keyInput = keyInput,
        )

        useCase
            .invoke()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    when (result) {
                        is ParseEnteredKeyUseCase.Result.Success -> {
                            log.debug {
                                "parseEnteredKey(): parsed_successfully:" +
                                        "\nparsed=${result.parsed}"
                            }

                            stateSubject.onNext(
                                State.SuccessfullyEntered(
                                    addedExtensions = setOf(
                                        GalleryExtension.MEMORIES,
                                        GalleryExtension.TEST,
                                    )
                                )
                            )
                        }

                        is ParseEnteredKeyUseCase.Result.Failure -> {
                            log.debug {
                                "parseEnteredKey(): parsing_failed:" +
                                        "\nfailure=$result"
                            }

                            when (result) {
                                ParseEnteredKeyUseCase.Result.Failure.INVALID ->
                                    keyError.value = KeyError.Invalid

                                ParseEnteredKeyUseCase.Result.Failure.DEVICE_MISMATCH ->
                                    keyError.value = KeyError.DeviceMismatch

                                ParseEnteredKeyUseCase.Result.Failure.EMAIL_MISMATCH ->
                                    keyError.value = KeyError.EmailMismatch

                                ParseEnteredKeyUseCase.Result.Failure.EXPIRED ->
                                    keyError.value = KeyError.Expired
                            }
                        }
                    }
                },
                onError = { error ->
                    log.error(error) {
                        "parseEnteredKey(): unexpected_error_occurred"
                    }
                    // TODO show floating error message
                }
            )
            .autoDispose(this)
    }

    fun onSuccessDoneClicked() {
        check(currentState is State.SuccessfullyEntered) {
            "Done button can only be clicked in the successfully entered state"
        }

        log.debug {
            "onSuccessDoneClicked(): finishing"
        }

        eventsSubject.onNext(Event.Finish)
    }

    sealed interface KeyError {
        object Invalid : KeyError
        object DeviceMismatch : KeyError
        object EmailMismatch : KeyError
        object Expired : KeyError
    }

    sealed interface State {
        object Entering : State

        class SuccessfullyEntered(
            val addedExtensions: Set<GalleryExtension>,
        ) : State
    }

    sealed interface Event {
        object Finish : Event
    }
}
