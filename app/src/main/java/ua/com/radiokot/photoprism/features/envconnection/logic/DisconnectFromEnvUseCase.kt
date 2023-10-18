package ua.com.radiokot.photoprism.features.envconnection.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.extension.kLogger
import java.io.File

/**
 * Clears the [envSessionHolder].
 * Clears [envAuthPersistence], [envSessionPersistence] and [cacheDirectories], if present.
 */
class DisconnectFromEnvUseCase(
    private val envSessionHolder: EnvSessionHolder,
    private val envSessionPersistence: ObjectPersistence<EnvSession>?,
    private val envAuthPersistence: ObjectPersistence<EnvAuth>?,
    private val cacheDirectories: Iterable<File>?,
) {
    private val log = kLogger("DisconnectFromEnvUseCase")

    operator fun invoke(): Completable = Completable.defer {
        envSessionHolder.clear()

        log.debug { "invoke(): session_holder_cleared" }

        envSessionPersistence?.clear()?.also {
            log.debug { "invoke(): session_persistence_cleared" }
        }

        envAuthPersistence?.clear()?.also {
            log.debug { "invoke(): auth_persistence_cleared" }
        }

        cacheDirectories?.forEach { cacheDir ->
            cacheDir.deleteRecursively()

            log.debug {
                "invoke(): cache_dir_deleted:" +
                        "\ndir=$cacheDir"
            }
        }

        Completable.complete()
    }
        .subscribeOn(Schedulers.io())
}
