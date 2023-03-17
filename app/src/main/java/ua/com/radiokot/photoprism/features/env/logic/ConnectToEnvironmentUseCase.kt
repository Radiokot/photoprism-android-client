package ua.com.radiokot.photoprism.features.env.logic

import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException
import ua.com.radiokot.photoprism.api.config.model.PhotoPrismClientConfig
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
    private lateinit var config: Config

    fun perform(): Single<EnvSession> {
        return getSessionId()
            .doOnSuccess { sessionId = it }
            .flatMap { getConfig() }
            .doOnSuccess { config = it }
            .map {
                EnvSession(
                    apiUrl = EnvConnection.rootUrlToApiUrl(config.rootUrl),
                    id = sessionId,
                    previewToken = config.previewToken,
                    downloadToken = config.downloadToken,
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

    private fun getConfig(): Single<Config> = {
        configServiceFactory(connection.apiUrl, sessionId)
            .getClientConfig()
            .let(::Config)
    }.toSingle()

    class InvalidPasswordException : RuntimeException()

    private data class Config(
        val previewToken: String,
        val downloadToken: String,
        val rootUrl: String,
    ) {
        constructor(source: PhotoPrismClientConfig) : this(
            previewToken = source.previewToken,
            downloadToken = source.downloadToken,
            rootUrl = source.siteUrl,
        )
    }
}