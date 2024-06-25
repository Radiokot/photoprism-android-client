package ua.com.radiokot.photoprism.features.envconnection.view.model

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import okhttp3.HttpUrl.Companion.toHttpUrl
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvIsNotPublicException
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.env.data.model.TfaCodeInvalidException
import ua.com.radiokot.photoprism.env.data.model.TfaRequiredException
import ua.com.radiokot.photoprism.env.data.model.WebPageInteractionRequiredException
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.basicAuth
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.extension.withMaskedCredentials
import ua.com.radiokot.photoprism.features.envconnection.logic.ConnectToEnvUseCase

class EnvConnectionViewModel(
    private val connectToEnvUseCaseFactory: ConnectToEnvUseCase.Factory,
    private val demoRootUrl: String,
) : ViewModel() {
    private val log = kLogger("EnvConnectionVM")

    val rootUrl = MutableLiveData<String>()
    val rootUrlError = MutableLiveData<Error.RootUrlError?>(null)
    val username = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val passwordError = MutableLiveData<Error.PasswordError?>(null)
    val isClientCertificateSelectionAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    val clientCertificateAlias = MutableLiveData<String?>()
    val isConnectButtonEnabled = MutableLiveData<Boolean>()
    private var isInitialized = false

    private val stateSubject = BehaviorSubject.createDefault<State>(State.Idle)
    val state = stateSubject.toMainThreadObservable()
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()

    private val canConnect: Boolean
        get() = stateSubject.value is State.Idle
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
        state.subscribe(updateConnectionButtonEnabled).autoDispose(this)

        rootUrl.observeForever {
            rootUrlError.value = null
            passwordError.value = null
        }
        val clearCredentialsErrors = { _: Any? ->
            passwordError.value = null
            if (rootUrlError.value == Error.RootUrlError.RequiresCredentials) {
                rootUrlError.value = null
            }
        }
        username.observeForever(clearCredentialsErrors)
        password.observeForever(clearCredentialsErrors)
        clientCertificateAlias.observeForever {
            if (rootUrlError.value is Error.RootUrlError.Inaccessible) {
                rootUrlError.value = null
            }
        }
    }

    fun initOnce(
        rootUrl: String?,
    ) {
        if (isInitialized) {
            return
        }

        rootUrl?.also(this.rootUrl::setValue)

        isInitialized = true
    }

    fun onTitleLongClicked() {
        if (stateSubject.value is State.Idle) {
            connectToDemo()
        }
    }

    private fun connectToDemo() {
        log.debug {
            "connectToDemo(): connecting_to_demo:" +
                    "\nrootUrl=$demoRootUrl"
        }

        rootUrl.value = demoRootUrl
        username.value = ""
        password.value = ""
        clientCertificateAlias.value = null

        if (canConnect) {
            connect()
        }
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
        eventsSubject.onNext(Event.OpenClientCertificateGuide)
    }

    fun onRootUrlGuideButtonClicked() {
        eventsSubject.onNext(Event.OpenConnectionGuide)
    }

    fun onWebViewerHandledRedirect() {
        if (canConnect) {
            log.debug {
                "onWebViewerHandledRedirect(): connecting_once_again"
            }

            connect()
        } else {
            log.warn {
                "onWebViewerHandledRedirect(): cant_connect_now"
            }
        }
    }

    fun onTfaCodeEntered(enteredTfaCode: String) {
        if (canConnect) {
            log.debug {
                "onTfaCodeEntered(): connecting_once_again"
            }

            connect(
                tfaCode = enteredTfaCode,
            )
        } else {
            log.warn {
                "onTfaCodeEntered(): cant_connect_now"
            }
        }
    }

    private fun connect(
        tfaCode: String? = null,
    ) {
        stateSubject.onNext(State.Connecting)

        val connectionParams: EnvConnectionParams = try {
            val rootHttpUrl = rootUrl.value!!.trim().toHttpUrl()

            EnvConnectionParams(
                rootUrl = rootHttpUrl.withMaskedCredentials(),
                clientCertificateAlias = clientCertificateAlias.value,
                httpAuth = rootHttpUrl.basicAuth,
            )
        } catch (e: Exception) {
            log.warn(e) { "connect(): connection_creation_failed" }

            stateSubject.onNext(State.Idle)
            rootUrlError.value = Error.RootUrlError.InvalidFormat

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
                    "\nauth=$auth," +
                    "\ntfaCode=$tfaCode"
        }

        connectToEnvUseCaseFactory
            .get(
                connectionParams = connectionParams,
                auth = auth,
                tfaCode = tfaCode,
            )
            .invoke()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                stateSubject.onNext(State.Connecting)
            }
            .doOnTerminate {
                stateSubject.onNext(State.Idle)
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
                            passwordError.value = Error.PasswordError.Invalid

                        is EnvIsNotPublicException ->
                            rootUrlError.value = Error.RootUrlError.RequiresCredentials

                        is WebPageInteractionRequiredException ->
                            // If proxy is blocking the access,
                            // let the user interact with it through a web page.
                            eventsSubject.onNext(
                                Event.OpenWebViewerForRedirectHandling(
                                    url = connectionParams.apiUrl.toString(),
                                )
                            )

                        is TfaRequiredException ->
                            eventsSubject.onNext(
                                Event.RequestTfaCodeInput
                            )

                        is TfaCodeInvalidException -> {
                            eventsSubject.onNext(
                                Event.ShowFloatingError(
                                    error = Error.InvalidTfaCode
                                )
                            )
                            eventsSubject.onNext(
                                Event.RequestTfaCodeInput
                            )
                        }

                        else ->
                            rootUrlError.value = Error.RootUrlError.Inaccessible(error.shortSummary)
                    }
                }
            )
            .autoDispose(this)
    }

    sealed interface Error {
        sealed interface RootUrlError : Error {
            class Inaccessible(val shortSummary: String) : RootUrlError
            object InvalidFormat : RootUrlError
            object RequiresCredentials : RootUrlError
        }

        sealed interface PasswordError : Error {
            object Invalid : PasswordError
        }

        object InvalidTfaCode : Error
    }

    sealed interface State {
        object Idle : State
        object Connecting : State
    }

    sealed interface Event {
        object GoToGallery : Event
        object ChooseClientCertificateAlias : Event
        object ShowMissingClientCertificatesNotice : Event
        object OpenConnectionGuide : Event
        object OpenClientCertificateGuide : Event
        class ShowFloatingError(val error: Error) : Event

        /**
         * Call [onWebViewerHandledRedirect] on successful result.
         */
        class OpenWebViewerForRedirectHandling(val url: String) : Event

        /**
         * Call [onTfaCodeEntered] on successful result.
         */
        object RequestTfaCodeInput : Event
    }
}
