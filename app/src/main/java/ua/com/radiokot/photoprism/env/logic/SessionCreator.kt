package ua.com.radiokot.photoprism.env.logic

import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession

interface SessionCreator {
    fun createSession(
        auth: EnvAuth,
        tfaCode: String? = null,
    ): EnvSession

    fun interface Factory {
        fun get(envConnectionParams: EnvConnectionParams): SessionCreator
    }
}
