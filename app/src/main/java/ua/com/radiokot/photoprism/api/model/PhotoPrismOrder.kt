package ua.com.radiokot.photoprism.api.model

import java.util.*

enum class PhotoPrismOrder {
    NEWEST,
    OLDEST,
    FAVORITES,
    NAME,
    PLACE,
    ;

    override fun toString(): String {
        return name.lowercase(Locale.ENGLISH)
    }
}