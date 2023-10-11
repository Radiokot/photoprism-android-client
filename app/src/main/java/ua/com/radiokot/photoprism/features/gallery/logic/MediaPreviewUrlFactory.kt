package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto

interface MediaPreviewUrlFactory {
    fun getSmallThumbnailUrl(hash: String): String
    fun getMediumThumbnailUrl(hash: String): String

    fun getImagePreview720Url(hash: String): String
    fun getImagePreview1280Url(hash: String): String
    fun getImagePreview1920Url(hash: String): String
    fun getImagePreview2048Url(hash: String): String
    fun getImagePreview2560Url(hash: String): String
    fun getImagePreview3840Url(hash: String): String
    fun getImagePreview4096Url(hash: String): String
    fun getImagePreview7680Url(hash: String): String

    fun getVideoPreviewUrl(mergedPhoto: PhotoPrismMergedPhoto): String
}
