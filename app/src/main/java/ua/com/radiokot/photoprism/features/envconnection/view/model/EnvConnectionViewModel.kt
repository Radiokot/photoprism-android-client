package ua.com.radiokot.photoprism.features.envconnection.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import retrofit2.HttpException
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.envconnection.data.model.EnvConnection
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.features.envconnection.logic.ConnectToEnvironmentUseCase

typealias ConnectUseCaseProvider = (connection: EnvConnection) -> ConnectToEnvironmentUseCase

class EnvConnectionViewModel(
    private val connectUseCaseProvider: ConnectUseCaseProvider,
) : ViewModel() {
    private val log = kLogger("EnvConnectionVM")

    val rootUrl = MutableLiveData<String>()
    val rootUrlError = MutableLiveData<RootUrlError?>(null)
    val isPublic = MutableLiveData(false)
    val username = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val passwordError = MutableLiveData<PasswordError?>(null)

    val state = MutableLiveData<State>(State.Idle)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject

    val isConnectButtonEnabled = MutableLiveData<Boolean>()
    val areCredentialsVisible = MutableLiveData<Boolean>()

    private val canConnect: Boolean
        get() = state.value is State.Idle
                && !rootUrl.value.isNullOrBlank()
                // Ignore URL error as it may be caused by the network
                && (
                isPublic.value == true
                        || isPublic.value == false
                        && !username.value.isNullOrBlank()
                        && !password.value.isNullOrEmpty()
                        && passwordError.value == null
                )

    init {
        val updateConnectionButtonEnabled = { _: Any? ->
            isConnectButtonEnabled.postValue(canConnect)
        }

        rootUrl.observeForever(updateConnectionButtonEnabled)
        rootUrlError.observeForever(updateConnectionButtonEnabled)
        isPublic.observeForever(updateConnectionButtonEnabled)
        username.observeForever(updateConnectionButtonEnabled)
        password.observeForever(updateConnectionButtonEnabled)
        passwordError.observeForever(updateConnectionButtonEnabled)
        state.observeForever(updateConnectionButtonEnabled)

        isPublic.observeForever {
            areCredentialsVisible.value = !it
            rootUrlError.value = null
        }

        rootUrl.observeForever {
            rootUrlError.value = null
            passwordError.value = null
        }
        username.observeForever { passwordError.value = null }
        password.observeForever { passwordError.value = null }
    }

    fun onConnectButtonClicked() {
        if (canConnect) {
            connect()
        }
    }

    fun onPasswordSubmitted(): Boolean {
        return if (canConnect) {
            connect()
            false
        } else {
            true
        }
    }

    private fun connect() {
        state.value = State.Connecting

        val connection: EnvConnection = try {
            EnvConnection(
                apiUrl = EnvConnection.rootUrlToApiUrl(rootUrl.value!!.trim()),
                auth =
                if (isPublic.value == true)
                    EnvAuth.Public
                else
                    EnvAuth.Credentials(
                        username = username.value!!.trim(),
                        password = password.value!!
                    )
            )
        } catch (e: Exception) {
            log.warn(e) { "connect(): connection_creation_failed" }

            state.value = State.Idle
            rootUrlError.value = RootUrlError.InvalidFormat

            return
        }

        log.debug {
            "connect(): connecting:" +
                    "\nconnection=$connection"
        }

        connectUseCaseProvider(connection)
            .perform()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                state.value = State.Connecting
            }
            .doOnTerminate {
                state.value = State.Idle
            }
            .subscribeBy(
                onSuccess = { session ->
                    log.debug {
                        "connect(): successfully_connected:" +
                                "\nsession=$session"
                    }

                    eventsSubject.onNext(Event.GoToGallery)
                },
                onError = { error ->
                    log.error(error) {
                        "connect(): error_occurred"
                    }

                    when (error) {
                        is InvalidCredentialsException ->
                            passwordError.value = PasswordError.Invalid
                        is HttpException ->
                            rootUrlError.value = RootUrlError.Inaccessible(error.code())
                        else ->
                            rootUrlError.value = RootUrlError.Inaccessible(null)
                    }
                }
            )
            .addToCloseables(this)
    }

    sealed interface RootUrlError {
        class Inaccessible(val code: Int?) : RootUrlError
        object InvalidFormat : RootUrlError
    }

    sealed interface PasswordError {
        object Invalid : PasswordError
    }

    sealed interface State {
        object Idle : State
        object Connecting : State
    }

    sealed interface Event {
        object GoToGallery : Event
    }
}