package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class SetGalleryMediaFavoriteUseCase {
    /**
     * @param currentGalleryMediaRepository repository currently used to present the gallery,
     * which will be updated on successful favorite set.
     */
    operator fun invoke(
        mediaUid: String,
        isFavorite: Boolean,
        currentGalleryMediaRepository: SimpleGalleryMediaRepository,
    ): Completable =
        currentGalleryMediaRepository
            .setFavorite(
                itemUid = mediaUid,
                isFavorite = isFavorite,
            )
}
