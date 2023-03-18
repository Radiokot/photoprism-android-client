package ua.com.radiokot.photoprism.env.logic

import ua.com.radiokot.photoprism.env.data.model.EnvAuth

interface SessionCreator {
    /**
     * @return created session ID
     */
    fun createSession(
        credentials: EnvAuth.Credentials,
    ): String
}