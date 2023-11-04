package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class SetGalleryMediaFavoriteUseCase(
    private val mediaUid: String,
    private val isFavorite: Boolean,
    private val currentGalleryMediaRepository: SimpleGalleryMediaRepository,
) {
    operator fun invoke(): Completable =
        currentGalleryMediaRepository
            .setFavorite(
                itemUid = mediaUid,
                isFavorite = isFavorite,
            )

    class Factory {
        /**
         * @param currentGalleryMediaRepository repository currently used to present the gallery,
         * which will be updated on successful favorite set.
         */
        fun get(
            mediaUid: String,
            isFavorite: Boolean,
            currentGalleryMediaRepository: SimpleGalleryMediaRepository,
        ): SetGalleryMediaFavoriteUseCase =
            SetGalleryMediaFavoriteUseCase(
                mediaUid,
                isFavorite,
                currentGalleryMediaRepository,
            )
    }
}
