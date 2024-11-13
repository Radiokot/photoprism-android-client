package ua.com.radiokot.photoprism.features.widgets.photoframe.data.model

import com.fasterxml.jackson.annotation.JsonCreator
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.util.LocalDate

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

    /**
     * [GalleryMedia.takenAtLocal] of this photo.
     */
    val takenAtLocal: LocalDate,
) {
    constructor(
        photo: GalleryMedia,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        previewUrl = previewUrlFactory.getImagePreviewUrl(
            previewHash = photo.hash,
            sizePx = PREVIEW_SIZE_PX,
        ),
        uid = photo.uid,
        takenAtLocal = photo.takenAtLocal,
    )

    private companion object {
        private const val PREVIEW_SIZE_PX = 720
    }
}
