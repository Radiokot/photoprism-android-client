package ua.com.radiokot.photoprism.env.logic

import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.kLogger

class UpdateExistingSessionAndPersistenceWithFreshTokens(
    private val existingSession: EnvSession,
    private val sessionPersistence: ObjectPersistence<EnvSession>?,
    private val persistenceUpdateIntervalMs: Long = 900 * 1000L,
) {
    private val log = kLogger("UpdateExistingSessionAndPersistenceWithFreshTokens")
    private var lastPersistenceUpdateMs = 0L

    operator fun invoke(
        previewToken: String?,
        downloadToken: String?,
    ) {
        if (previewToken != null) {
            existingSession.previewToken = previewToken
        }
        if (downloadToken != null) {
            existingSession.downloadToken = downloadToken
        }

        // Since tokens come quite frequent,
        // do not write to the persistence every time.
        synchronized(this) {
            val now = System.currentTimeMillis()
            if (now - lastPersistenceUpdateMs >= persistenceUpdateIntervalMs) {
                sessionPersistence?.saveItem(existingSession)
                lastPersistenceUpdateMs = now

                log.debug {
                    "invoke(): persistence_updated:" +
                            "\npreviewToken=$previewToken," +
                            "\ndownloadToken=$downloadToken"
                }
            }
        }
    }
}
