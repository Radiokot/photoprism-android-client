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
import ua.com.radiokot.photoprism.util.LocalDate
import ua.com.radiokot.photoprism.util.PagedCollectionLoader
import java.util.Calendar
import java.util.GregorianCalendar

class GetMemoriesUseCase(
    private val photoPrismClientConfigService: PhotoPrismClientConfigService,
    private val photoPrismPhotosService: PhotoPrismPhotosService,
) {
    operator fun invoke(): Single<Any> =
        getPastYears()
            .flatMap { years ->
                Maybe.concat(
                    years
                        .map { year ->
                            getPastYearMemories(year)
                                .map { items ->
                                    year to "uid:" + items.joinToString(
                                        separator = "|",
                                        transform = PhotoPrismMergedPhoto::uid,
                                    )
                                }
                        }
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

    /**
     * "This time N years ago" – few photos around this time in the specified past [year].
     */
    private fun getPastYearMemories(year: Int): Maybe<List<PhotoPrismMergedPhoto>> {
        val thisDayLocalDate = LocalDate().getCalendar().run {
            set(Calendar.YEAR, year)
            LocalDate(time)
        }

        return getItemsForMemories(
            searchConfig = SearchConfig.DEFAULT.copy(
                mediaTypes = MEDIA_TYPES,
                beforeLocal = LocalDate(thisDayLocalDate.time + PAST_YEAR_MARGIN_MS),
                afterLocal = LocalDate(thisDayLocalDate.time - PAST_YEAR_MARGIN_MS),
            )
        )
            .flatMapMaybe { items ->
                if (items.isEmpty())
                    Maybe.empty()
                else
                    Maybe.just(items)
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

    private companion object {
        private val MEDIA_TYPES = setOf(
            GalleryMedia.TypeName.IMAGE,
            GalleryMedia.TypeName.VIDEO,
            GalleryMedia.TypeName.LIVE,
        )
        private const val PAST_YEAR_MARGIN_MS = 3 * 86400000L // N days
        private const val MAX_MEMORIES_ITEMS_COUNT = 6
        private val MEMORIES_ITEMS_COMPARATOR =
            compareByDescending(PhotoPrismMergedPhoto::favorite)
                .thenByDescending(PhotoPrismMergedPhoto::quality)
    }
}
