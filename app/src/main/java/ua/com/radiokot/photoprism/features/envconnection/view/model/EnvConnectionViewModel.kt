package ua.com.radiokot.photoprism.features.envconnection.view.model

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvIsNotPublicException
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.features.envconnection.logic.ConnectToEnvUseCase

typealias ConnectUseCaseProvider = (
    connection: EnvConnectionParams,
    auth: EnvAuth,
) -> ConnectToEnvUseCase

class EnvConnectionViewModel(
    private val connectUseCaseProvider: ConnectUseCaseProvider,
    private val clientCertificatesGuideUrl: String,
    private val rootUrlGuideUrl: String,
) : ViewModel() {
    private val log = kLogger("EnvConnectionVM")

    val rootUrl = MutableLiveData<String>()
    val rootUrlError = MutableLiveData<RootUrlError?>(null)
    val username = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val passwordError = MutableLiveData<PasswordError?>(null)
    val isClientCertificateSelectionAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    val clientCertificateAlias = MutableLiveData<String?>()

    val state = MutableLiveData<State>(State.Idle)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOn(AndroidSchedulers.mainThread())

    val isConnectButtonEnabled = MutableLiveData<Boolean>()

    private val canConnect: Boolean
        get() = state.value is State.Idle
                // Ignore URL error as it may be caused by the network
                && !rootUrl.value.isNullOrBlank()
                // Check the credentials if the username is entered.
                && (username.value.isNullOrBlank() || !password.value.isNullOrEmpty() && passwordError.value == null)


    init {
        val updateConnectionButtonEnabled = { _: Any? ->
            isConnectButtonEnabled.postValue(canConnect)
        }

        rootUrl.observeForever(updateConnectionButtonEnabled)
        rootUrlError.observeForever(updateConnectionButtonEnabled)
        username.observeForever(updateConnectionButtonEnabled)
        password.observeForever(updateConnectionButtonEnabled)
        passwordError.observeForever(updateConnectionButtonEnabled)
        state.observeForever(updateConnectionButtonEnabled)

        rootUrl.observeForever {
            rootUrlError.value = null
            passwordError.value = null
        }
        val clearCredentialsErrors = { _: Any? ->
            passwordError.value = null
            if (rootUrlError.value == RootUrlError.RequiresCredentials) {
                rootUrlError.value = null
            }
        }
        username.observeForever(clearCredentialsErrors)
        password.observeForever(clearCredentialsErrors)
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

    fun onCertificateFieldClicked() {
        if (!isClientCertificateSelectionAvailable) {
            error("Certificate field can't be clicked if the selection is not available")
        }

        log.debug {
            "onCertificateFieldClicked(): requesting_client_certificate_alias"
        }

        eventsSubject.onNext(Event.ChooseClientCertificateAlias)
    }

    fun onCertificateAliasChosen(alias: String) {
        log.debug {
            "onCertificateAliasChosen(): alias_chosen:" +
                    "\nalias=$alias"
        }

        // Post from the background thread.
        clientCertificateAlias.postValue(alias)
    }

    fun onNoCertificatesAvailable() {
        log.debug {
            "onNoCertificatesAvailable(): showing_notice"
        }

        eventsSubject.onNext(Event.ShowMissingClientCertificatesNotice)
    }

    fun onCertificateClearButtonClicked() {
        clientCertificateAlias.value = null
    }

    fun onCertificateLearnMoreButtonClicked() {
        eventsSubject.onNext(
            Event.OpenUrl(
                url = clientCertificatesGuideUrl,
            )
        )
    }

    fun onRootUrlGuideButtonClicked() {
        eventsSubject.onNext(
            Event.OpenUrl(
                url = rootUrlGuideUrl,
            )
        )
    }

    private fun connect() {
        state.value = State.Connecting

        val connectionParams: EnvConnectionParams = try {
            EnvConnectionParams(
                apiUrl = EnvConnectionParams.rootUrlToApiUrl(rootUrl.value!!.trim()),
                clientCertificateAlias = clientCertificateAlias.value,
            )
        } catch (e: Exception) {
            log.warn(e) { "connect(): connection_creation_failed" }

            state.value = State.Idle
            rootUrlError.value = RootUrlError.InvalidFormat

            return
        }

        val username = username.value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        val password = password.value
            ?.takeIf(String::isNotEmpty)

        val auth =
            if (username != null && password != null)
                EnvAuth.Credentials(
                    username = username,
                    password = password,
                )
            else
                EnvAuth.Public

        log.debug {
            "connect(): connecting:" +
                    "\nconnectionParams=$connectionParams," +
                    "\nauth=$auth"
        }

        connectUseCaseProvider(connectionParams, auth)
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
                        is EnvIsNotPublicException ->
                            rootUrlError.value = RootUrlError.RequiresCredentials
                        else ->
                            rootUrlError.value = RootUrlError.Inaccessible(error.shortSummary)
                    }
                }
            )
            .addToCloseables(this)
    }

    sealed interface RootUrlError {
        class Inaccessible(val shortSummary: String) : RootUrlError
        object InvalidFormat : RootUrlError
        object RequiresCredentials : RootUrlError
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
        object ChooseClientCertificateAlias : Event
        object ShowMissingClientCertificatesNotice : Event
        class OpenUrl(val url: String) : Event
    }
}