package ua.com.radiokot.photoprism.features.gallery.data.storage

import androidx.collection.LruCache
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.model.PagingOrder
import ua.com.radiokot.photoprism.base.data.storage.SimplePagedDataRepository
import ua.com.radiokot.photoprism.extension.*
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import java.util.*

class SimpleGalleryMediaRepository(
    private val photoPrismPhotosService: PhotoPrismPhotosService,
    private val thumbnailUrlFactory: MediaPreviewUrlFactory,
    private val downloadUrlFactory: MediaFileDownloadUrlFactory,
    val query: String?,
    pageLimit: Int,
) : SimplePagedDataRepository<GalleryMedia>(
    pagingOrder = PagingOrder.DESC,
    pageLimit = pageLimit,
) {
    private val log = kLogger("SimpleGalleryMediaRepo")

    override fun getPage(
        limit: Int,
        cursor: String?,
        order: PagingOrder
    ): Single<DataPage<GalleryMedia>> {
        val galleryMediaItems = mutableListOf<GalleryMedia>()
        var nextCursor = cursor
        var offset = 0
        var pageIsLast = false

        val loadPage = {
            offset = nextCursor?.toInt() ?: 0

            log.debug {
                "getPage(): loading_page:" +
                        "\noffset=$offset," +
                        "\blimit=$pageLimit"
            }

            photoPrismPhotosService.getPhotos(
                count = pageLimit,
                offset = offset,
                q = query,
                order = when (pagingOrder) {
                    PagingOrder.DESC -> PhotoPrismOrder.NEWEST
                    PagingOrder.ASC -> PhotoPrismOrder.OLDEST
                }
            )
        }
            .toSingle()
            .map { photoPrismPhotos ->
                val filesCount = photoPrismPhotos.sumOf { it.files.size }
                pageIsLast = filesCount < limit

                log.debug {
                    "getPage(): raw_page_loaded:" +
                            "\nfilesCount=${photoPrismPhotos.sumOf { it.files.size }}," +
                            "\npageIsLast=$pageIsLast"
                }

                photoPrismPhotos.mapSuccessful {
                    GalleryMedia(
                        source = it,
                        previewUrlFactory = thumbnailUrlFactory,
                        downloadUrlFactory = downloadUrlFactory,
                    )
                }
            }
            .doOnSuccess { successfullyLoadedItems ->
                galleryMediaItems.addAll(successfullyLoadedItems)

                // Load extra data to fulfill the requested page limit.
                nextCursor = (limit + offset).toString()

                log.debug {
                    "getPage(): page_loaded:" +
                            "\nsuccessfullyLoadedItemsCount=${successfullyLoadedItems.size}," +
                            "\nexpectedCount=$pageLimit"
                }
            }

        return loadPage
            .repeatUntil { pageIsLast || galleryMediaItems.size >= pageLimit }
            .ignoreElements()
            .toSingle {
                log.debug {
                    "getPage(): loaded_enough_data:" +
                            "\nitemsCount=${galleryMediaItems.size}," +
                            "\nlimit=$limit"
                }

                DataPage(
                    items = galleryMediaItems,
                    nextCursor = nextCursor.checkNotNull {
                        "The cursor must be defined at this moment"
                    },
                    isLast = pageIsLast,
                )
            }
    }

    fun getNewestAndOldestDates(): Maybe<Pair<Date, Date>> {
        val getNewestDate = {
            photoPrismPhotosService.getPhotos(
                count = 1,
                offset = 0,
                q = query,
                order = PhotoPrismOrder.NEWEST
            )
                .firstOrNull()
                ?.takenAt
                ?.let(GalleryMedia.Companion::parsePhotoPrismDate)
        }.toMaybe()

        val getOldestDate = {
            photoPrismPhotosService.getPhotos(
                count = 1,
                offset = 0,
                q = query,
                order = PhotoPrismOrder.OLDEST
            )
                .firstOrNull()
                ?.takenAt
                ?.let(GalleryMedia.Companion::parsePhotoPrismDate)
        }.toMaybe()

        return Maybe.zip(
            getNewestDate,
            getOldestDate,
            ::Pair
        )
            .subscribeOn(Schedulers.io())
    }

    override fun toString(): String {
        return "SimpleGalleryMediaRepository(query=$query)"
    }

    class Factory(
        private val photoPrismPhotosService: PhotoPrismPhotosService,
        private val thumbnailUrlFactory: MediaPreviewUrlFactory,
        private val downloadUrlFactory: MediaFileDownloadUrlFactory,
        private val pageLimit: Int,
    ) {
        private val cache = LruCache<String, SimpleGalleryMediaRepository>(5)

        fun getForSearch(config: SearchConfig): SimpleGalleryMediaRepository {
            val queryBuilder = StringBuilder()

            if (config.mediaTypes.isNotEmpty()) {
                queryBuilder.append(
                    " type:${
                        config.mediaTypes.joinToString("|") { it.value }
                    }"
                )
            }

            if (config.userQuery != null) {
                queryBuilder.append(" ${config.userQuery}")
            }

            val query = queryBuilder.toString()
                .trim()
                .takeUnless(String::isNullOrBlank)

            return get(query)
        }

        fun get(query: String?): SimpleGalleryMediaRepository {
            val key = "q:$query"

            return cache[key]
                ?: SimpleGalleryMediaRepository(
                    photoPrismPhotosService = photoPrismPhotosService,
                    thumbnailUrlFactory = thumbnailUrlFactory,
                    downloadUrlFactory = downloadUrlFactory,
                    query = query,
                    pageLimit = pageLimit,
                ).also {
                    cache.put(key, it)
                }
        }
    }
}