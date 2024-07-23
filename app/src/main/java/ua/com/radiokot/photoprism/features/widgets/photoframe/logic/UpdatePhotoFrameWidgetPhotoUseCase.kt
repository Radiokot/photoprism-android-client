package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.ViewableAsImage
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences

class UpdatePhotoFrameWidgetPhotoUseCase(
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
) {
    private val log = kLogger("UpdatePhotoFrameWidgetPhotoUseCase")

    operator fun invoke(
        widgetId: Int,
    ): Completable {
        val galleryMediaRepository = galleryMediaRepositoryFactory.get(
            searchConfig = widgetsPreferences.getSearchConfig(widgetId)
        )

        return galleryMediaRepository
            .updateIfNotFreshDeferred()
            .andThen(pickAndSaveRandomPhoto(widgetId, galleryMediaRepository))
    }

    private fun pickAndSaveRandomPhoto(
        widgetId: Int,
        repository: SimpleGalleryMediaRepository,
    ): Completable = {
        // TODO: Implement good picking algorithm.
        val randomPhoto = repository.itemsList.random()

        log.debug {
            "pickAndSaveRandomPhoto(): picked:" +
                    "\nrandomPhoto=$randomPhoto"
        }

        val previewUrl = (randomPhoto.media as? ViewableAsImage)
            .checkNotNull {
                "The repository must only contain ViewableAsImage"
            }
            .getImagePreviewUrl(PREVIEW_SIZE_PX)

        widgetsPreferences.setPhotoUrl(
            widgetId = widgetId,
            photoUrl = previewUrl,
        )
    }.toCompletable().subscribeOn(Schedulers.io())

    private companion object {
        private const val PREVIEW_SIZE_PX = 800
    }
}
