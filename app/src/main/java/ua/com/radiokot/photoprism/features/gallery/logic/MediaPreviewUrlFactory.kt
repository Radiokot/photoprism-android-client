package ua.com.radiokot.photoprism.features.gallery.logic

interface MediaPreviewUrlFactory {
    fun getSmallThumbnailUrl(hash: String): String
    fun getMediumThumbnailUrl(hash: String): String

    fun getHdPreviewUrl(hash: String): String
}