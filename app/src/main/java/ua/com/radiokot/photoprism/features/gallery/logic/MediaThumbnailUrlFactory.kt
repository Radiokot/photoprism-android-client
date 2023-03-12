package ua.com.radiokot.photoprism.features.gallery.logic

interface MediaThumbnailUrlFactory {
    fun getSmallThumbnailUrl(hash: String): String
    fun getMediumThumbnailUrl(hash: String): String
}