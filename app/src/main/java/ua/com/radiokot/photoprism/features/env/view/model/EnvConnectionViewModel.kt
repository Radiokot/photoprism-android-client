package ua.com.radiokot.photoprism.features.env.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection
import java.util.concurrent.TimeUnit

class EnvConnectionViewModel(

) : ViewModel() {
    private val log = kLogger("EnvConnectionVM")

    val rootUrl = MutableLiveData<CharSequence?>()
    val rootUrlError = MutableLiveData<RootUrlError?>(null)
    val isPublic = MutableLiveData(false)
    val username = MutableLiveData<CharSequence?>()
    val password = MutableLiveData<CharSequence?>()
    val passwordError = MutableLiveData<PasswordError?>(null)

    val state = MutableLiveData<State>(State.Idle)

    val isConnectButtonEnabled = MutableLiveData<Boolean>()
    val areCredentialsVisible = MutableLiveData<Boolean>()

    private val canConnect: Boolean
        get() = state.value is State.Idle
                && !rootUrl.value.isNullOrBlank()
                && rootUrlError.value == null
                && (
                isPublic.value == true
                        || isPublic.value == false
                        && !username.value.isNullOrBlank()
                        && !password.value.isNullOrEmpty()
                        && passwordError.value == null
                )

    fun init() {
        val updateConnectionButtonEnabled = { _: Any? ->
            isConnectButtonEnabled.value = canConnect
        }

        rootUrl.observeForever(updateConnectionButtonEnabled)
        rootUrlError.observeForever(updateConnectionButtonEnabled)
        isPublic.observeForever(updateConnectionButtonEnabled)
        username.observeForever(updateConnectionButtonEnabled)
        password.observeForever(updateConnectionButtonEnabled)
        passwordError.observeForever(updateConnectionButtonEnabled)
        state.observeForever(updateConnectionButtonEnabled)

        isPublic.observeForever { areCredentialsVisible.value = !it }

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
            EnvConnection.fromRootUrl(
                rootUrl = rootUrl.value!!.toString().trim(),
                auth =
                if (isPublic.value == true)
                    EnvConnection.Auth.Public
                else
                    EnvConnection.Auth.Credentials(
                        username = username.value!!.toString().trim(),
                        password = password.value!!.toString()
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

        Observable.timer(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                state.value = State.Idle
                rootUrlError.value = RootUrlError.Inaccessible(123)
            }
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
}