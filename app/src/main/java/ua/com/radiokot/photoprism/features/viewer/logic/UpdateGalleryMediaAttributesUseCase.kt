package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class UpdateGalleryMediaAttributesUseCase {
    /**
     * @param currentGalleryMediaRepository repository currently used to present the gallery,
     * which will be updated on successful update.
     *
     * @param isFavorite to update favorite state, **null** to omit
     * @param isPrivate to update private state, **null** to omit
     */
    operator fun invoke(
        mediaUids: Collection<String>,
        currentGalleryMediaRepository: SimpleGalleryMediaRepository,
        isFavorite: Boolean? = null,
        isPrivate: Boolean? = null,
    ): Completable =
        // Use more compatible method for single item update.
        if (mediaUids.size == 1)
            currentGalleryMediaRepository
                .updateAttributes(
                    itemUid = mediaUids.first(),
                    isFavorite = isFavorite,
                    isPrivate = isPrivate,
                )
        else
            currentGalleryMediaRepository
                .updateAttributes(
                    itemUids = mediaUids,
                    isFavorite = isFavorite,
                    isPrivate = isPrivate,
                )
}
