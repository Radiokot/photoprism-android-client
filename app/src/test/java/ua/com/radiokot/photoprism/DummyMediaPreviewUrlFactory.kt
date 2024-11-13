package ua.com.radiokot.photoprism

import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

class DummyMediaPreviewUrlFactory : MediaPreviewUrlFactory {
    override fun getThumbnailUrl(thumbnailHash: String, sizePx: Int): String =
        "https://preview.local/thumb/$thumbnailHash/$sizePx"

    override fun getImagePreviewUrl(previewHash: String, sizePx: Int): String =
        "https://preview.local/preview/$previewHash/$sizePx"

    override fun getVideoPreviewUrl(
        previewHash: String,
        videoFileHash: String?,
        videoFileCodec: String?
    ): String =
        "https://preview.local/video/$previewHash"
}
