package ua.com.radiokot.photoprism.features.gallery.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class ArchiveGalleryMediaUseCase(
    private val mediaUids: Collection<String>,
    private val currentGalleryMediaRepository: SimpleGalleryMediaRepository,
) {
    operator fun invoke(): Completable =
        currentGalleryMediaRepository
            .archive(
                itemUids = mediaUids
            )

    class Factory {
        /**
         * @param currentGalleryMediaRepository repository currently used to present the gallery,
         * which will be updated on successful archiving.
         */
        fun get(
            mediaUid: String,
            currentGalleryMediaRepository: SimpleGalleryMediaRepository,
        ): ArchiveGalleryMediaUseCase =
            get(
                setOf(mediaUid),
                currentGalleryMediaRepository,
            )

        /**
         * @param currentGalleryMediaRepository repository currently used to present the gallery,
         * which will be updated on successful archiving.
         */
        fun get(
            mediaUids: Collection<String>,
            currentGalleryMediaRepository: SimpleGalleryMediaRepository,
        ): ArchiveGalleryMediaUseCase =
            ArchiveGalleryMediaUseCase(
                mediaUids,
                currentGalleryMediaRepository,
            )
    }
}
