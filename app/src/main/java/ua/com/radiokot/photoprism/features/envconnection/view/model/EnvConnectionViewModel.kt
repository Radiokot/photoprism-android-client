package ua.com.radiokot.photoprism.features.envconnection.view.model

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvIsNotPublicException
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
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

    private val stateSubject = BehaviorSubject.createDefault<State>(State.Idle)
    val state = stateSubject.toMainThreadObservable()
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()

    val isConnectButtonEnabled = MutableLiveData<Boolean>()

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
            if (rootUrlError.value == RootUrlError.RequiresCredentials) {
                rootUrlError.value = null
            }
        }
        username.observeForever(clearCredentialsErrors)
        password.observeForever(clearCredentialsErrors)
        clientCertificateAlias.observeForever {
            if (rootUrlError.value is RootUrlError.Inaccessible) {
                rootUrlError.value = null
            }
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
        eventsSubject.onNext(
            Event.OpenClientCertificateGuide(
                url = clientCertificatesGuideUrl,
            )
        )
    }

    fun onRootUrlGuideButtonClicked() {
        eventsSubject.onNext(
            Event.OpenConnectionGuide(
                url = rootUrlGuideUrl,
            )
        )
    }

    private fun connect() {
        stateSubject.onNext(State.Connecting)

        val connectionParams: EnvConnectionParams = try {
            EnvConnectionParams(
                rootUrlString = rootUrl.value!!.trim(),
                clientCertificateAlias = clientCertificateAlias.value,
            )
        } catch (e: Exception) {
            log.warn(e) { "connect(): connection_creation_failed" }

            stateSubject.onNext(State.Idle)
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
                            passwordError.value = PasswordError.Invalid
                        is EnvIsNotPublicException ->
                            rootUrlError.value = RootUrlError.RequiresCredentials
                        else ->
                            rootUrlError.value = RootUrlError.Inaccessible(error.shortSummary)
                    }
                }
            )
            .autoDispose(this)
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
        class OpenConnectionGuide(val url: String) : Event
        class OpenClientCertificateGuide(val url: String) : Event
    }
}
