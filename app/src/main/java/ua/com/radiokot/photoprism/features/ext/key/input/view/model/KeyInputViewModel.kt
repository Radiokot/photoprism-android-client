package ua.com.radiokot.photoprism.features.ext.key.input.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.ext.key.input.logic.ParseEnteredKeyUseCase
import ua.com.radiokot.photoprism.features.ext.model.GalleryExtension

class KeyInputViewModel(
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

        try {
            val parsedKey = useCase.invoke()

            stateSubject.onNext(
                State.SuccessfullyEntered(
                    addedExtensions = setOf(GalleryExtension.MEMORIES)
                )
            )
        } catch (_: ParseEnteredKeyUseCase.InvalidFormatException) {
            keyError.value = KeyError.InvalidFormat
        } catch (_: ParseEnteredKeyUseCase.ExpiredException) {
            keyError.value = KeyError.Expired
        } catch (_: ParseEnteredKeyUseCase.EmailMismatchException) {
            keyError.value = KeyError.EmailMismatch
        } catch (_: ParseEnteredKeyUseCase.DeviceMismatchException) {
            keyError.value = KeyError.DeviceMismatch
        }
    }

    sealed interface KeyError {
        object InvalidFormat : KeyError
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

    sealed interface Event
}
