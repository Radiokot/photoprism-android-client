package ua.com.radiokot.photoprism.env.logic

import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.env.data.model.EnvSession

class UpdateExistingSessionAndPersistenceOnRenewal(
    private val existingSession: EnvSession,
    private val sessionPersistence: ObjectPersistence<EnvSession>?,
) {
    operator fun invoke(newSession: EnvSession) {
        existingSession.id = newSession.id
        existingSession.downloadToken = newSession.downloadToken
        existingSession.previewToken = newSession.previewToken
        sessionPersistence?.saveItem(existingSession)
    }
}
