package ua.com.radiokot.photoprism.features.widgets.photoframe.data.model

import com.fasterxml.jackson.annotation.JsonCreator
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.ViewableAsImage

data class PhotoFrameWidgetPhoto
@JsonCreator
constructor(
    /**
     * Decent size preview URL of this photo.
     */
    val previewUrl: String,

    /**
     * [GalleryMedia.uid] of this photo.
     */
    val uid: String,
) {
    constructor(photo: GalleryMedia) : this(
        previewUrl = (photo.media as? ViewableAsImage)
            ?.getImagePreviewUrl(PREVIEW_SIZE_PX)
            ?: throw IllegalArgumentException("The photo must be ViewableAsImage"),
        uid = photo.uid,
    )

    private companion object {
        private const val PREVIEW_SIZE_PX = 720
    }
}
