package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class SetGalleryMediaPrivateUseCase {
    /**
     * @param currentGalleryMediaRepository repository currently used to present the gallery,
     * which will be updated on successful private set.
     */
    operator fun invoke(
        mediaUid: String,
        isPrivate: Boolean,
        currentGalleryMediaRepository: SimpleGalleryMediaRepository,
    ): Completable =
        currentGalleryMediaRepository
            .setPrivate(
                itemUid = mediaUid,
                isPrivate = isPrivate,
            )
}
