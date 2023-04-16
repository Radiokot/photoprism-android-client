package ua.com.radiokot.photoprism.env.data.storage

import ua.com.radiokot.photoprism.env.data.model.EnvSession

interface EnvSessionHolder {
    fun set(session: EnvSession)
    fun clear()
    val isSet: Boolean
}