package ua.com.radiokot.photoprism.features.gallery.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class DeleteGalleryMediaUseCase {

    /**
     * @param currentGalleryMediaRepository repository currently used to present the gallery,
     * which will be updated on successful removal.
     */
    operator fun invoke(
        mediaUids: Collection<String>,
        currentGalleryMediaRepository: SimpleGalleryMediaRepository,
    ): Completable =
        currentGalleryMediaRepository
            .delete(
                itemUids = mediaUids
            )

    /**
     * @param currentGalleryMediaRepository repository currently used to present the gallery,
     * which will be updated on successful removal.
     */
    operator fun invoke(
        mediaUid: String,
        currentGalleryMediaRepository: SimpleGalleryMediaRepository,
    ) = invoke(
        setOf(mediaUid),
        currentGalleryMediaRepository
    )
}
