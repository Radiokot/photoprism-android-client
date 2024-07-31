package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetPhoto
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences

class UpdatePhotoFrameWidgetPhotoUseCase(
    private val allowedMediaTypes: Set<GalleryMedia.TypeName>,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
) {
    private val log = kLogger("UpdatePhotoFrameWidgetPhotoUseCase")

    operator fun invoke(
        widgetId: Int,
    ): Completable {
        val galleryMediaRepository = galleryMediaRepositoryFactory.get(
            searchConfig = widgetsPreferences.getSearchConfig(widgetId)
                ?: SearchConfig.DEFAULT.withOnlyAllowedMediaTypes(allowedMediaTypes)
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
        val randomPhoto = PhotoFrameWidgetPhoto(
            photo = repository.itemsList.random()
        )

        log.debug {
            "pickAndSaveRandomPhoto(): picked:" +
                    "\nrandomPhoto=$randomPhoto," +
                    "\nwidgetId=$widgetId"
        }

        widgetsPreferences.setPhoto(
            widgetId = widgetId,
            photo = randomPhoto,
        )
    }.toCompletable().subscribeOn(Schedulers.io())
}
