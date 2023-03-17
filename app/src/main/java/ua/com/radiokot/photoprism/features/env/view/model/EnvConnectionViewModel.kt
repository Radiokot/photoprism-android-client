package ua.com.radiokot.photoprism.features.env.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EnvConnectionViewModel(

) : ViewModel() {
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
        username.observeForever(updateConnectionButtonEnabled)
        password.observeForever(updateConnectionButtonEnabled)
        passwordError.observeForever(updateConnectionButtonEnabled)
        state.observeForever(updateConnectionButtonEnabled)

        isPublic.observeForever { areCredentialsVisible.value = !it }

        rootUrl.observeForever { rootUrlError.value = null }
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