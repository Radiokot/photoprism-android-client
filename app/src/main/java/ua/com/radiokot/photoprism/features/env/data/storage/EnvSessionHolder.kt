package ua.com.radiokot.photoprism.features.env.data.storage

import ua.com.radiokot.photoprism.features.env.data.model.EnvSession

interface EnvSessionHolder {
    fun set(session: EnvSession)
    val isSet: Boolean
}