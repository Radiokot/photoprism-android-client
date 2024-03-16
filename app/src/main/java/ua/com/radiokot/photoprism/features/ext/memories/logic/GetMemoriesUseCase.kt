package ua.com.radiokot.photoprism.features.ext.memories.logic

import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.ext.memories.data.model.Memory
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Fetches memories relevant at the moment.
 */
class GetMemoriesUseCase(
    private val photoPrismClientConfigService: PhotoPrismClientConfigService,
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
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
            ?.sorted() // For memories to be returned in chronological order.
            ?.asSequence()
            ?.filter { it < currentYear }
            ?: emptySequence()
    }.toSingle()

    private fun getThisDayInThePastMemories(year: Int): Maybe<Memory> {
        val localCalendar = LocalDate().getCalendar()

        return getItemsForMemories(
            searchConfig = SearchConfig.DEFAULT.copy(
                mediaTypes = MEDIA_TYPES,
                userQuery = "day:${localCalendar[Calendar.DAY_OF_MONTH]-3} " +
                        "month:${localCalendar[Calendar.MONTH] + 1} " +
                        "year:$year",
            )
        )
            .flatMapMaybe { photoPrismMergedPhotos ->
                if (photoPrismMergedPhotos.isEmpty())
                    return@flatMapMaybe Maybe.empty()

                Maybe.just(
                    Memory(
                        typeData = Memory.TypeData.ThisDayInThePast(
                            year = year,
                        ),
                        searchQuery = photoPrismMergedPhotos.searchQuery,
                        previewHash = photoPrismMergedPhotos.first().hash,
                        previewUrlFactory = previewUrlFactory,
                    )
                )
            }
    }

    private fun getItemsForMemories(searchConfig: SearchConfig): Single<List<GalleryMedia>> {
        val repository = galleryMediaRepositoryFactory.create(
            params = SimpleGalleryMediaRepository.Params(searchConfig),
            pageLimit = MAX_ITEMS_TO_LOAD,
        )

        return repository
            .updateDeferred()
            .toSingle {
                // Take only a limited number of the most suitable items.
                repository
                    .itemsList
                    .filter {
                        !it.title.lowercase().contains("screenshot")
                    }
                    .sortedWith(MEMORIES_ITEMS_COMPARATOR)
                    .take(MAX_MEMORIES_ITEMS_COUNT)
            }
    }

    private val Collection<GalleryMedia>.searchQuery: String
        get() = "uid:" + joinToString(
            separator = "|",
            transform = GalleryMedia::uid,
        )

    private companion object {
        private val MEDIA_TYPES = setOf(
            GalleryMedia.TypeName.IMAGE,
            GalleryMedia.TypeName.VIDEO,
            GalleryMedia.TypeName.LIVE,
        )
        private const val MAX_MEMORIES_ITEMS_COUNT = 6
        private const val MAX_ITEMS_TO_LOAD = 150
        private val MEMORIES_ITEMS_COMPARATOR = compareByDescending(GalleryMedia::isFavorite)
    }
}
