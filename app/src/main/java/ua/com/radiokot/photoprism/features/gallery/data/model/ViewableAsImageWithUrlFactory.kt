package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

class ViewableAsImageWithUrlFactory(
    private val hash: String,
    private val mediaPreviewUrlFactory: MediaPreviewUrlFactory,
) : ViewableAsImage {
    override fun getImagePreviewUrl(viewSizePx: Int): String =
        when {
            viewSizePx < 1000 ->
                mediaPreviewUrlFactory.getImagePreview720Url(hash)

            viewSizePx < 1500 ->
                mediaPreviewUrlFactory.getImagePreview1280Url(hash)

            viewSizePx < 2000 ->
                mediaPreviewUrlFactory.getImagePreview1920Url(hash)

            viewSizePx < 2500 ->
                mediaPreviewUrlFactory.getImagePreview2048Url(hash)

            viewSizePx < 4000 ->
                mediaPreviewUrlFactory.getImagePreview3840Url(hash)

            viewSizePx < 4500 ->
                mediaPreviewUrlFactory.getImagePreview4096Url(hash)

            else ->
                mediaPreviewUrlFactory.getImagePreview7680Url(hash)
        }
}
