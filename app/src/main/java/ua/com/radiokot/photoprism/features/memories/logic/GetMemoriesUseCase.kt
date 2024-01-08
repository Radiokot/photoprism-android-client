package ua.com.radiokot.photoprism.features.memories.logic

import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.memories.data.model.Memory
import ua.com.radiokot.photoprism.util.LocalDate
import ua.com.radiokot.photoprism.util.PagedCollectionLoader
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Fetches memories relevant at the moment.
 */
class GetMemoriesUseCase(
    private val photoPrismClientConfigService: PhotoPrismClientConfigService,
    private val photoPrismPhotosService: PhotoPrismPhotosService,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) {
    operator fun invoke(): Single<List<Memory>> =
        getPastYears()
            .flatMap { years ->
                Maybe.concat(
                    years
                        .map(::getThisDayInThePastMemories)
                        .asIterable()
                ).toList()
            }

    /**
     * @return all the past years that should be checked for memories.
     */
    private fun getPastYears(): Single<Sequence<Int>> = {
        val currentYear = GregorianCalendar.getInstance()[Calendar.YEAR]

        photoPrismClientConfigService.getClientConfig()
            .years
            ?.sortedDescending()
            ?.asSequence()
            ?.filter { it < currentYear }
            ?: emptySequence()
    }.toSingle()

    private fun getThisDayInThePastMemories(year: Int): Maybe<Memory.ThisDayInThePast> {
        val localCalendar = LocalDate().getCalendar()

        return getItemsForMemories(
            searchConfig = SearchConfig.DEFAULT.copy(
                mediaTypes = MEDIA_TYPES,
                userQuery = "day:${localCalendar[Calendar.DAY_OF_MONTH]} " +
                        "month:${localCalendar[Calendar.MONTH] + 1} " +
                        "year:$year",
            )
        )
            .flatMapMaybe { photoPrismMergedPhotos ->
                if (photoPrismMergedPhotos.isEmpty())
                    return@flatMapMaybe Maybe.empty()

                Memory.ThisDayInThePast(
                    year = year,
                    searchQuery = photoPrismMergedPhotos.searchQuery,
                    smallThumbnailUrl = photoPrismMergedPhotos.smallThumbnailUrl,
                ).let { Maybe.just(it) }
            }
    }

    private fun getItemsForMemories(searchConfig: SearchConfig): Single<List<PhotoPrismMergedPhoto>> {
        val query = searchConfig.getPhotoPrismQuery()

        return PagedCollectionLoader(
            pageProvider = { cursor ->
                {
                    val offset = cursor?.toInt() ?: 0
                    val count = 40

                    val items = photoPrismPhotosService.getMergedPhotos(
                        count = count,
                        offset = offset,
                        q = query,
                    )

                    DataPage(
                        items = items,
                        nextCursor = (offset + count).toString(),
                        isLast = items.size < count,
                    )
                }.toSingle()
            }
        )
            .loadAll()
            .map { mergedPhotos ->
                // Take only a limited number of the most suitable items.
                mergedPhotos
                    .sortedWith(MEMORIES_ITEMS_COMPARATOR)
                    .subList(0, mergedPhotos.size.coerceAtMost(MAX_MEMORIES_ITEMS_COUNT))
                    .distinctBy(PhotoPrismMergedPhoto::uid)
            }
    }

    private val Collection<PhotoPrismMergedPhoto>.searchQuery: String
        get() = "uid:" + joinToString(
            separator = "|",
            transform = PhotoPrismMergedPhoto::uid,
        )

    private val Collection<PhotoPrismMergedPhoto>.smallThumbnailUrl: String
        get() = previewUrlFactory.getSmallThumbnailUrl(
            hash = first().hash
        )

    private companion object {
        private val MEDIA_TYPES = setOf(
            GalleryMedia.TypeName.IMAGE,
            GalleryMedia.TypeName.VIDEO,
            GalleryMedia.TypeName.LIVE,
        )
        private const val MAX_MEMORIES_ITEMS_COUNT = 6
        private val MEMORIES_ITEMS_COMPARATOR =
            compareByDescending(PhotoPrismMergedPhoto::favorite)
                .thenByDescending(PhotoPrismMergedPhoto::quality)
    }
}
