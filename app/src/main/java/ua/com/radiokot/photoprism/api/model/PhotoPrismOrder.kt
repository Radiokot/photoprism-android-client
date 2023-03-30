package ua.com.radiokot.photoprism.api.model

import java.util.*

enum class PhotoPrismOrder {
    NEWEST,
    OLDEST,
    ;

    override fun toString(): String {
        return name.lowercase(Locale.ENGLISH)
    }
}