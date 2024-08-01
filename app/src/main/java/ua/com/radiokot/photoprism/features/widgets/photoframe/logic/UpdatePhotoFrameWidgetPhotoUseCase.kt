package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetPhoto
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.util.DbscanClustering
import ua.com.radiokot.photoprism.util.LocalDate

class UpdatePhotoFrameWidgetPhotoUseCase(
    private val allowedMediaTypes: Set<GalleryMedia.TypeName>,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
) {
    private val log = kLogger("UpdatePhotoFrameWidgetPhotoUseCase")

    operator fun invoke(
        widgetId: Int,
    ): Completable {
        val widgetSearchConfig =
            (widgetsPreferences.getSearchConfig(widgetId) ?: SearchConfig.DEFAULT)
                .withOnlyAllowedMediaTypes(allowedMediaTypes)

        return galleryMediaRepositoryFactory
            .get(
                searchConfig = widgetSearchConfig,
            )
            // Get the date range of the given search.
            // Further operations are not executed if this search finds nothing
            // hence there is no date range.
            .getNewestAndOldestLocalDates()
            // Pick a random date within this range.
            .map { (newestDate, oldestDate) ->
                LocalDate((oldestDate.time..newestDate.time).random())
                    .also { pickedDate ->
                        log.debug {
                            "invoke(): picked_date:" +
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
                    pageLimit = MAX_ITEMS_TO_LOAD,
                )
            }
            // Load enough items.
            .flatMapSingle { randomDateRepository ->
                randomDateRepository
                    .updateIfNotFreshDeferred()
                    .toSingle { randomDateRepository.itemsList }
            }
            .observeOn(Schedulers.io())
            .map { repositoryItems ->
                check(repositoryItems.isNotEmpty()) {
                    "The repository to pick from not be empty"
                }

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
                    .sortedWith(PREFERABLE_ITEM_COMPARATOR)
                    .first()
            }
            .map(::PhotoFrameWidgetPhoto)
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

    private companion object {
        private const val MAX_ITEMS_TO_LOAD = 150
        private const val TIME_CLUSTERING_DISTANCE_MS = 15_000L
        private val PREFERABLE_ITEM_COMPARATOR = compareByDescending(GalleryMedia::isFavorite)
    }
}
