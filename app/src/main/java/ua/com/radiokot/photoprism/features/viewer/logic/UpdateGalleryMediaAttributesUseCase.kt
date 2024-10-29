package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class UpdateGalleryMediaAttributesUseCase {
    /**
     * @param currentGalleryMediaRepository repository currently used to present the gallery,
     * which will be updated on successful update.
     *
     * @param isFavorite to update favorite state, **null** to omit
     * @param  isFavorite to update private state, **null** to omit
     */
    operator fun invoke(
        mediaUid: String,
        currentGalleryMediaRepository: SimpleGalleryMediaRepository,
        isFavorite: Boolean? = null,
        isPrivate: Boolean? = null,
    ): Completable =
        currentGalleryMediaRepository
            .updateAttributes(
                itemUid = mediaUid,
                isFavorite = isFavorite,
                isPrivate = isPrivate,
            )
}
