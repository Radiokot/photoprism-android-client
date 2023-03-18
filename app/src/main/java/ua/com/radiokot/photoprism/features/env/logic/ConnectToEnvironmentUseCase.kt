package ua.com.radiokot.photoprism.features.env.logic

import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.api.config.model.PhotoPrismClientConfig
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection
import ua.com.radiokot.photoprism.features.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.features.env.data.storage.EnvSessionHolder

typealias PhotoPrismConfigServiceFactory =
            (apiUrl: String, sessionId: String) -> PhotoPrismClientConfigService

/**
 * Creates [EnvConnection] for the given [connection].
 * On success, sets the session to the [envSessionHolder] and [envSessionPersistence], if present.
 *
 * @see InvalidCredentialsException
 */
class ConnectToEnvironmentUseCase(
    private val connection: EnvConnection,
    private val configServiceFactory: PhotoPrismConfigServiceFactory,
    private val sessionCreator: SessionCreator,
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
                sessionCreator
                    .createSession(
                        apiUrl = connection.apiUrl,
                        credentials = connection.auth
                    )
        }
    }
        .toSingle()

    private fun getConfig(): Single<Config> = {
        configServiceFactory(connection.apiUrl, sessionId)
            .getClientConfig()
            .let(::Config)
    }.toSingle()

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