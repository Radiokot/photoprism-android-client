package ua.com.radiokot.photoprism.features.env.logic

import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection

interface SessionCreator {
    /**
     * @return created session ID
     */
    fun createSession(
        credentials: EnvConnection.Auth.Credentials,
    ): String
}