package ua.com.radiokot.photoprism.features.gallery.logic

interface MediaPreviewUrlFactory {
    fun getSmallThumbnailUrl(hash: String): String
    fun getMediumThumbnailUrl(hash: String): String

    fun getPreview720Url(hash: String): String
    fun getPreview1280Url(hash: String): String
    fun getPreview1920Url(hash: String): String
    fun getPreview2048Url(hash: String): String
    fun getPreview2560Url(hash: String): String
    fun getPreview3840Url(hash: String): String
    fun getPreview4096Url(hash: String): String
    fun getPreview7680Url(hash: String): String

    fun getMp4PreviewUrl(hash: String): String
}
