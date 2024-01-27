package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

class WithThumbnailFromUrlFactory(
    private val hash: String,
    private val mediaPreviewUrlFactory: MediaPreviewUrlFactory,
): WithThumbnail {

    override fun getThumbnailUrl(viewSizePx: Int): String =
        when {
            viewSizePx < 180 ->
                mediaPreviewUrlFactory.getThumbnail100Url(hash)

            viewSizePx < 400 ->
                mediaPreviewUrlFactory.getThumbnail224Url(hash)

            else ->
                mediaPreviewUrlFactory.getThumbnail500Url(hash)
        }
}
