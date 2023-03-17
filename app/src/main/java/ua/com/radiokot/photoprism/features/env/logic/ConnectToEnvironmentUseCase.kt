package ua.com.radiokot.photoprism.features.env.logic

import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionCredentials
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection
import ua.com.radiokot.photoprism.features.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.env.data.storage.EnvSessionHolder
import java.net.HttpURLConnection

typealias PhotoPrismSessionServiceFactory =
            (apiUrl: String) -> PhotoPrismSessionService

typealias PhotoPrismConfigServiceFactory =
            (apiUrl: String, sessionId: String) -> PhotoPrismClientConfigService

/**
 * Creates [EnvConnection] for the given [connection].
 * On success, sets the session to the [envSessionHolder] and [envSessionPersistence], if present.
 *
 * @see InvalidPasswordException
 */
class ConnectToEnvironmentUseCase(
    private val connection: EnvConnection,
    private val sessionServiceFactory: PhotoPrismSessionServiceFactory,
    private val configServiceFactory: PhotoPrismConfigServiceFactory,
    private val envSessionHolder: EnvSessionHolder?,
    private val envSessionPersistence: ObjectPersistence<EnvSession>?,
) {
    private lateinit var sessionId: String
    private lateinit var previewToken: String
    private lateinit var downloadToken: String

    fun perform(): Single<EnvSession> {
        return getSessionId()
            .doOnSuccess { sessionId = it }
            .flatMap { getTokens() }
            .doOnSuccess { tokens ->
                previewToken = tokens.first
                downloadToken = tokens.second
            }
            .map {
                EnvSession(
                    apiUrl = connection.apiUrl,
                    id = sessionId,
                    previewToken = previewToken,
                    downloadToken = downloadToken,
                )
            }
            .doOnSuccess { session ->
                envSessionHolder?.set(session)
                envSessionPersistence?.saveItem(session)
            }
    }

    private fun getSessionId(): Single<String> = {
        when (connection.auth) {
            is EnvConnection.Auth.Public ->
                "public"
            is EnvConnection.Auth.Credentials ->
                sessionServiceFactory(connection.apiUrl)
                    .createSession(
                        PhotoPrismSessionCredentials(
                            username = connection.auth.username,
                            password = connection.auth.password,
                        )
                    )
                    .id
        }
    }
        .toSingle()
        .onErrorResumeNext { error ->
            if (error is HttpException && error.code() == HttpURLConnection.HTTP_UNAUTHORIZED)
                Single.error(InvalidPasswordException())
            else
                Single.error(error)
        }

    private fun getTokens(): Single<Pair<String, String>> = {
        configServiceFactory(connection.apiUrl, sessionId)
            .getClientConfig()
            .let { it.previewToken to it.downloadToken }
    }.toSingle()

    class InvalidPasswordException : RuntimeException()
}