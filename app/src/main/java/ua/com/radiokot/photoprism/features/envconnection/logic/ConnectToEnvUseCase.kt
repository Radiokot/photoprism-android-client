package ua.com.radiokot.photoprism.features.envconnection.logic

import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.api.config.model.PhotoPrismClientConfig
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvIsNotPublicException
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.env.data.model.ProxyBlockingAccessException
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.env.logic.SessionCreator
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle

typealias PhotoPrismConfigServiceFactory =
            (envConnectionParams: EnvConnectionParams, sessionId: String) -> PhotoPrismClientConfigService

/**
 * Creates [EnvSession] for the given [EnvConnectionParams] and [EnvAuth].
 * On success, sets the session to the [envSessionHolder] and [envSessionPersistence]
 * and the [auth] to the [envAuthPersistence], if present.
 *
 * @see InvalidCredentialsException
 * @see EnvIsNotPublicException
 * @see ProxyBlockingAccessException
 */
class ConnectToEnvUseCase(
    private val connectionParams: EnvConnectionParams,
    private val auth: EnvAuth,
    private val configServiceFactory: PhotoPrismConfigServiceFactory,
    private val sessionCreator: SessionCreator,
    private val envSessionHolder: EnvSessionHolder?,
    private val envSessionPersistence: ObjectPersistence<EnvSession>?,
    private val envAuthPersistence: ObjectPersistence<EnvAuth>?,
) {
    private val log = kLogger("ConnectToEnvUseCase")

    private lateinit var session: EnvSession

    operator fun invoke(): Single<EnvSession> {
        return getSession()
            .doOnSuccess {
                session = it

                log.debug {
                    "perform(): got_session:" +
                            "\nid=${it.id}"
                }
            }
            .flatMap { checkEnv() }
            .onErrorResumeNext { error ->
                Single.error(
                    if (ProxyBlockingAccessException.THROWABLE_PREDICATE.test(error))
                        ProxyBlockingAccessException()
                    else
                        error
                )
            }
            .doOnSuccess {
                log.debug { "perform(): env_checked" }
            }
            .map { session }
            .doOnSuccess {
                log.debug {
                    "perform(): successfully_created_session:" +
                            "\nsession=$session"
                }

                updateHoldersAndPersistence()
            }
    }

    private fun getSession(): Single<EnvSession> = {
        sessionCreator.createSession(
            auth = auth,
        )
    }
        .toSingle()

    private fun checkEnv(): Single<Boolean> =
        if (auth is EnvAuth.Public)
        // If the auth is public, check that the env is actually public.
            getEnvClientConfig()
                .map { photoPrismConfig ->
                    if (!photoPrismConfig.public) {
                        throw EnvIsNotPublicException()
                    } else {
                        true
                    }
                }
        else
        // Otherwise, there is currently nothing to check.
            Single.just(true)

    private fun getEnvClientConfig(): Single<PhotoPrismClientConfig> = {
        configServiceFactory(connectionParams, session.id)
            .getClientConfig()
    }.toSingle()

    private fun updateHoldersAndPersistence() {
        envSessionHolder?.apply {
            set(session)

            log.debug {
                "updateHoldersAndPersistence(): session_holder_set:" +
                        "\nholder=$this"
            }
        }

        envSessionPersistence?.apply {
            saveItem(session)

            log.debug {
                "updateHoldersAndPersistence(): session_saved_to_persistence:" +
                        "\npersistence=$this"
            }
        }

        envAuthPersistence?.apply {
            saveItem(auth)

            log.debug {
                "updateHoldersAndPersistence(): auth_saved_to_persistence:" +
                        "\npersistence=$this"
            }
        }
    }

    fun interface Factory {
        fun get(
            connection: EnvConnectionParams,
            auth: EnvAuth,
        ): ConnectToEnvUseCase
    }
}
