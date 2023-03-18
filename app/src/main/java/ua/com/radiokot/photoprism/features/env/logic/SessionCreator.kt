package ua.com.radiokot.photoprism.features.env.logic

import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection

interface SessionCreator {
    /**
     * @return created session ID
     */
    fun createSession(
        apiUrl: String,
        credentials: EnvConnection.Auth.Credentials
    ): String
}