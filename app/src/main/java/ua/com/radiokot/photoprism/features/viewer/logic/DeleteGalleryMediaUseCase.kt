package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class DeleteGalleryMediaUseCase(
    private val mediaUid: String,
    private val currentGalleryMediaRepository: SimpleGalleryMediaRepository,
) {
    operator fun invoke(): Completable =
        currentGalleryMediaRepository
            .delete(
                itemUids = setOf(mediaUid)
            )

    class Factory {
        /**
         * @param currentGalleryMediaRepository repository currently used to present the gallery,
         * which will be updated on successful removal.
         */
        fun get(
            mediaUid: String,
            currentGalleryMediaRepository: SimpleGalleryMediaRepository,
        ): DeleteGalleryMediaUseCase =
            DeleteGalleryMediaUseCase(
                mediaUid,
                currentGalleryMediaRepository,
            )
    }
}
