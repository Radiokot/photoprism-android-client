package ua.com.radiokot.photoprism

import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

class DummyMediaPreviewUrlFactory: MediaPreviewUrlFactory {
    override fun getThumbnail100Url(hash: String): String =
        "http://preview.local/thumb-100-$hash"

    override fun getThumbnail224Url(hash: String): String =
        "http://preview.local/thumb-224-$hash"

    override fun getThumbnail500Url(hash: String): String =
        "http://preview.local/thumb-500-$hash"

    override fun getImagePreview720Url(hash: String): String =
        "http://preview.local/prev-720-$hash"

    override fun getImagePreview1280Url(hash: String): String =
        "http://preview.local/prev-1280-$hash"

    override fun getImagePreview1920Url(hash: String): String =
        "http://preview.local/prev-1920-$hash"

    override fun getImagePreview2048Url(hash: String): String =
        "http://preview.local/prev-2048-$hash"

    override fun getImagePreview2560Url(hash: String): String =
        "http://preview.local/prev-2560-$hash"

    override fun getImagePreview3840Url(hash: String): String =
        "http://preview.local/prev-3840-$hash"

    override fun getImagePreview4096Url(hash: String): String =
        "http://preview.local/prev-4096-$hash"

    override fun getImagePreview7680Url(hash: String): String =
        "http://preview.local/prev-7680-$hash"

    override fun getVideoPreviewUrl(mergedPhoto: PhotoPrismMergedPhoto): String =
        "http://preview.local/prev-video-${mergedPhoto.hash}"
}
