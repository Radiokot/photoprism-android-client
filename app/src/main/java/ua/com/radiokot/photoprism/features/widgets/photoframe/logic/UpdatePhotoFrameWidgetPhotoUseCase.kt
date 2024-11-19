package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.HttpException
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMaybe
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetPhoto
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.util.DbscanClustering
import ua.com.radiokot.photoprism.util.LocalDate
import java.net.HttpURLConnection
import kotlin.random.Random

class UpdatePhotoFrameWidgetPhotoUseCase(
    private val allowedMediaTypes: Set<GalleryMedia.TypeName>,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val photoPrismPhotosService: PhotoPrismPhotosService,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) {
    private val log = kLogger("UpdatePhotoFrameWidgetPhotoUseCase")

    operator fun invoke(
        widgetId: Int,
    ): Completable {
        val widgetSearchConfig =
            (widgetsPreferences.getSearchConfig(widgetId) ?: SearchConfig.DEFAULT)
                .withOnlyAllowedMediaTypes(allowedMediaTypes)

        // Try picking a random media by order=random,
        // but fall back to the time-based picking if the server is old.
        return pickRandomMediaByOrder(widgetSearchConfig)
            .onErrorResumeNext { error ->
                if (error is HttpException && error.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
                    log.debug {
                        "invoke(): falling_back_to_time_method"
                    }

                    pickRandomMediaByTime(
                        widgetSearchConfig = widgetSearchConfig,
                        widgetId = widgetId,
                    )
                } else {
                    Maybe.error(error)
                }
            }
            .map { galleryMedia ->
                PhotoFrameWidgetPhoto(
                    photo = galleryMedia,
                    previewUrlFactory = previewUrlFactory,
                )
            }
            .doOnSuccess { pickedPhoto ->
                log.debug {
                    "invoke(): picked_photo:" +
                            "\nphoto=$pickedPhoto," +
                            "\nwidgetId=$widgetId"
                }

                widgetsPreferences.setPhoto(
                    widgetId = widgetId,
                    photo = pickedPhoto,
                )
            }
            .ignoreElement()
    }

    private fun pickRandomMediaByOrder(
        widgetSearchConfig: SearchConfig,
    ): Maybe<GalleryMedia> = {
        photoPrismPhotosService.getMergedPhotos(
            count = 1,
            offset = 0,
            order = PhotoPrismOrder.RANDOM,
            q = widgetSearchConfig.getPhotoPrismQuery(),
        )
            .firstOrNull()
            ?.let(GalleryMedia::fromPhotoPrism)
    }.toMaybe().subscribeOn(Schedulers.io())

    private fun pickRandomMediaByTime(
        widgetSearchConfig: SearchConfig,
        widgetId: Int,
    ): Maybe<GalleryMedia> =
        galleryMediaRepositoryFactory
            .get(
                searchConfig = widgetSearchConfig,
            )
            // Get the date range of the given search.
            // Further operations are not executed if this search finds nothing
            // hence there is no date range.
            .getNewestAndOldestLocalDates()
            // Pick a random date as a starting point.
            // To increase probability of picking recent photos,
            // extend the range to the future.
            // Add 1 ms to the picked time so when used as "before" date later
            // it doesn't filter out the only item.
            .map { (newestDate, oldestDate) ->
                val timeRangeLength = newestDate.time - oldestDate.time
                val extendedTimeRangeLength = timeRangeLength + timeRangeLength / 2
                val randomTime = oldestDate.time + Random.nextLong(extendedTimeRangeLength + 1)
                LocalDate(randomTime + 1)
                    .also { pickedDate ->
                        log.debug {
                            "pickRandomMediaByTime(): picked_date:" +
                                    "\nnewest:$newestDate," +
                                    "\noldest:$oldestDate," +
                                    "\npicked=$pickedDate," +
                                    "\nwidgetId=$widgetId"
                        }
                    }
            }
            // Create a repository starting at this random date
            // to pick the final photo somewhere close.
            .map { randomDate ->
                galleryMediaRepositoryFactory.create(
                    SimpleGalleryMediaRepository.Params(
                        searchConfig = widgetSearchConfig.copy(
                            beforeLocal = randomDate
                        ),
                    ),
                    pageLimit = TIME_MAX_ITEMS_TO_LOAD,
                )
            }
            // Load enough items.
            .flatMapSingle { randomDateRepository ->
                randomDateRepository
                    .updateIfNotFreshDeferred()
                    .toSingle { randomDateRepository.itemsList }
            }
            .observeOn(Schedulers.io())
            .filter(List<*>::isNotEmpty)
            .map { repositoryItems ->
                repositoryItems
                    // Group items by time taken.
                    // Here we do not filter out garbage as for memories,
                    // because the user controls the widget search query.
                    .let { filteredItems ->
                        DbscanClustering(filteredItems) { it.takenAtLocal.time }
                            .cluster(
                                maxDistance = TIME_CLUSTERING_DISTANCE_MS,
                                minClusterSize = 1,
                            )
                    }
                    // Pick a random group (cluster).
                    .random()
                    // From the group items, pick the most preferred one.
                    .sortedWith(TIME_PREFERABLE_CLUSTER_ITEM_COMPARATOR)
                    .first()
            }

    private companion object {
        private const val TIME_MAX_ITEMS_TO_LOAD = 150
        private const val TIME_CLUSTERING_DISTANCE_MS = 15_000L
        private val TIME_PREFERABLE_CLUSTER_ITEM_COMPARATOR =
            compareByDescending(GalleryMedia::isFavorite)
    }
}
