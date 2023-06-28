package ua.com.radiokot.photoprism.env.logic

import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvSession

interface SessionCreator {
    fun createSession(
        auth: EnvAuth,
    ): EnvSession
}
