package ua.com.radiokot.photoprism.features.gallery.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class DeleteGalleryMediaUseCase(
    private val mediaUids: Collection<String>,
    private val currentGalleryMediaRepository: SimpleGalleryMediaRepository,
) {
    operator fun invoke(): Completable =
        currentGalleryMediaRepository
            .delete(
                itemUids = mediaUids
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
            get(
                setOf(mediaUid),
                currentGalleryMediaRepository,
            )

        /**
         * @param currentGalleryMediaRepository repository currently used to present the gallery,
         * which will be updated on successful removal.
         */
        fun get(
            mediaUids: Collection<String>,
            currentGalleryMediaRepository: SimpleGalleryMediaRepository,
        ): DeleteGalleryMediaUseCase =
            DeleteGalleryMediaUseCase(
                mediaUids,
                currentGalleryMediaRepository,
            )
    }
}
