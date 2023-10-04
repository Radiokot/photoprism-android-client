package ua.com.radiokot.photoprism.features.envconnection.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.extension.kLogger

/**
 * Clears the [envSessionHolder].
 * Clears [envAuthPersistence] and [envSessionPersistence], if present.
 */
class DisconnectFromEnvUseCase(
    private val envSessionHolder: EnvSessionHolder,
    private val envSessionPersistence: ObjectPersistence<EnvSession>?,
    private val envAuthPersistence: ObjectPersistence<EnvAuth>?,
) {
    private val log = kLogger("DisconnectFromEnvUseCase")

    operator fun invoke() = Completable.defer {
        envSessionHolder.clear()

        log.debug { "perform(): session_holder_cleared" }

        envSessionPersistence?.clear()?.also {
            log.debug { "perform(): session_persistence_cleared" }
        }

        envAuthPersistence?.clear()?.also {
            log.debug { "perform(): auth_persistence_cleared" }
        }

        Completable.complete()
    }
        .subscribeOn(Schedulers.io())
}
