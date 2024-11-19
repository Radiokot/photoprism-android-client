package ua.com.radiokot.photoprism.api.model

import java.util.Locale

enum class PhotoPrismOrder {
    NEWEST,

    OLDEST,

    /**
     * Added in the PhotoPrism release of May 2, 2023.
     */
    RANDOM,
    ;

    override fun toString(): String {
        return name.lowercase(Locale.ENGLISH)
    }
}
