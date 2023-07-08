package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.os.Parcelable
import androidx.collection.LruCache
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.model.PagingOrder
import ua.com.radiokot.photoprism.base.data.storage.Repository
import ua.com.radiokot.photoprism.base.data.storage.SimplePagedDataRepository
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.mapSuccessful
import ua.com.radiokot.photoprism.extension.toMaybe
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.model.formatPhotoPrismDate
import ua.com.radiokot.photoprism.features.gallery.data.model.parsePhotoPrismDate
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import java.util.Date

/**
 * @param pageLimit target limit setting the minimum number of items in the page.
 * The actual pages are bigger due to the PhotoPrism pagination workaround.
 */
class SimpleGalleryMediaRepository(
    private val photoPrismPhotosService: PhotoPrismPhotosService,
    private val thumbnailUrlFactory: MediaPreviewUrlFactory,
    private val downloadUrlFactory: MediaFileDownloadUrlFactory,
    private val webUrlFactory: MediaWebUrlFactory,
    val params: Params,
    pageLimit: Int,
) : SimplePagedDataRepository<GalleryMedia>(
    pagingOrder = PagingOrder.DESC,
    pageLimit = pageLimit,
) {
    private val log = kLogger("SimpleGalleryMediaRepo")

    // See .addNewPageItems for explanation.
    private val itemsByUid = mutableMapOf<String, GalleryMedia>()

    override fun getPage(
        limit: Int,
        cursor: String?,
        order: PagingOrder
    ): Single<DataPage<GalleryMedia>> {
        // Must not be changed to a set. Do not distinct items.
        // See .addNewPageItems for explanation.
        val collectedGalleryMediaItems = mutableListOf<GalleryMedia>()

        var nextCursor = cursor
        var offset = 0
        var pageIsLast = false

        // Lookahead limit is set to be greater than the actual limit
        // assuming we'll always have some merged items with multiple files.
        // In this case, increased limit helps avoiding the second network call
        // and leads to a faster loading.
        val lookaheadLimit = limit * 2

        val loadPage = {
            offset = nextCursor?.toInt() ?: 0

            log.debug {
                "getPage(): loading_page:" +
                        "\noffset=$offset," +
                        "\nlimit=$limit," +
                        "\nlookaheadLimit=$lookaheadLimit"
            }

            photoPrismPhotosService.getMergedPhotos(
                count = lookaheadLimit,
                offset = offset,
                q = params.query,
                order = when (pagingOrder) {
                    PagingOrder.DESC -> PhotoPrismOrder.NEWEST
                    PagingOrder.ASC -> PhotoPrismOrder.OLDEST
                }
            )
        }
            .toSingle()
            .map { photoPrismPhotos ->
                val filesCount = photoPrismPhotos.sumOf { it.files.size }
                pageIsLast = filesCount < lookaheadLimit

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
                        webUrlFactory = webUrlFactory,
                    )
                }
            }
            .doOnSuccess { successfullyLoadedItems ->
                collectedGalleryMediaItems.addAll(successfullyLoadedItems)

                // Load extra data to fulfill the requested page limit.
                // The limit is set for the items, but PhotoPrism counts files.
                // Merged items break limits.
                // See .addNewPageItems for more.
                nextCursor = (lookaheadLimit + offset).toString()

                log.debug {
                    "getPage(): page_loaded:" +
                            "\nsuccessfullyLoadedItemsCount=${successfullyLoadedItems.size}," +
                            "\nexpectedCount=$limit"
                }
            }

        return loadPage
            .repeatUntil { pageIsLast || collectedGalleryMediaItems.size >= limit }
            .ignoreElements()
            .toSingle {
                log.debug {
                    "getPage(): loaded_enough_data:" +
                            "\nitemsCount=${collectedGalleryMediaItems.size}," +
                            "\nlimit=$limit"
                }

                DataPage(
                    items = collectedGalleryMediaItems,
                    nextCursor = nextCursor.checkNotNull {
                        "The cursor must be defined at this moment"
                    },
                    isLast = pageIsLast,
                )
            }
    }

    private var newestAndOldestDates: Pair<Date, Date>? = null
    fun getNewestAndOldestDates(): Maybe<Pair<Date, Date>> {
        val loadedDates = newestAndOldestDates
        if (loadedDates != null) {
            return Maybe.just(loadedDates)
        }

        val getNewestDate = {
            photoPrismPhotosService.getMergedPhotos(
                count = 1,
                offset = 0,
                q = params.query,
                order = PhotoPrismOrder.NEWEST
            )
                .firstOrNull()
                ?.takenAtLocal
                ?.let(::parsePhotoPrismDate)
        }.toMaybe()

        val getOldestDate = {
            photoPrismPhotosService.getMergedPhotos(
                count = 1,
                offset = 0,
                q = params.query,
                order = PhotoPrismOrder.OLDEST
            )
                .firstOrNull()
                ?.takenAtLocal
                ?.let(::parsePhotoPrismDate)
        }.toMaybe()

        return Maybe.zip(
            getNewestDate,
            getOldestDate,
            ::Pair
        )
            .doOnSuccess { newestAndOldestDates = it }
            .subscribeOn(Schedulers.io())
    }

    override fun addNewPageItems(page: DataPage<GalleryMedia>) {
        page.items.forEach { item ->
            // Precise post filter by the "before" date, workaround for PhotoPrism filtering.
            // Do not add items, taken at or after the specified time.
            if (params.postFilterBefore != null && item.takenAtLocal >= params.postFilterBefore) {
                return@forEach
            }

            if (itemsByUid.containsKey(item.uid)) {
                // If this item is already loaded, just merge the files. Why?
                // Scenario:
                // 1. Loaded a page of merged photos. PhotoPrism page limit limits number of files, not photos;
                // 2. Last page photo is happened to be a video, it has 2 files (preview and video);
                // 3. Because of the limit, only the first file is returned (preview);
                // 4. Now in the repository we have an item with only one file.
                //    Until the next page is loaded, this item is in some way broken;
                // 5. Loaded the next page. The first photo is the same video, but now
                //    it contains only the second file (video);
                // 5. Ha, we've caught that! Merging the files;
                // 6. Now the existing item has all the required files.
                //
                // More reliable workaround is not to fetch files from the merged photos pages,
                // but to load them on demand through the /view endpoint.
                // But I think this doesn't worth it.

                itemsByUid.getValue(item.uid).mergeFiles(item.files)

                log.debug {
                    "addNewPageItems(): merged_files:" +
                            "\nitemUid=${item.uid}"
                }
            } else {
                mutableItemsList.add(item)
                itemsByUid[item.uid] = item
            }
        }
    }

    override fun update(): Completable {
        newestAndOldestDates = null
        itemsByUid.clear()
        return super.update()
    }

    override fun invalidate() {
        newestAndOldestDates = null
        super.invalidate()
    }

    override fun toString(): String {
        return "SimpleGalleryMediaRepository(params=$params)"
    }

    /**
     * @param query user query
     * @param postFilterBefore time to apply post filtering of the items,
     * as PhotoPrism is incapable of time filtering.
     */
    @Parcelize
    data class Params(
        val query: String? = null,
        val postFilterBefore: Date? = null,
    ) : Parcelable

    class Factory(
        private val photoPrismPhotosService: PhotoPrismPhotosService,
        private val thumbnailUrlFactory: MediaPreviewUrlFactory,
        private val downloadUrlFactory: MediaFileDownloadUrlFactory,
        private val webUrlFactory: MediaWebUrlFactory,
        private val pageLimit: Int,
    ) {
        private val cache = LruCache<String, SimpleGalleryMediaRepository>(10)

        fun getForSearch(config: SearchConfig): SimpleGalleryMediaRepository {
            val queryBuilder = StringBuilder()

            // User query goes first, hence all the other params override the input.
            queryBuilder.append(" ${config.userQuery}")

            // If mediaTypes are not specified, all the types are allowed and no filter is added.
            // If they are empty, nothing is allowed (empty search results).
            if (config.mediaTypes != null) {
                if (config.mediaTypes.isEmpty()) {
                    queryBuilder.append(" type:nothing")
                } else {
                    queryBuilder.append(
                        " type:${
                            config.mediaTypes.joinToString("|") { it.value }
                        }"
                    )
                }
            }

            if (config.before != null) {
                val timeToNextDay = DAY_MS - config.before.time % DAY_MS
                // PhotoPrism "before" filter does not take into account the time.
                // "before:2023-04-30T22:57:32Z" is treated like "2023-04-30T00:00:00Z".
                // Redundancy is needed whenever the requested before date is in between UTC midnights,
                // so 2023-04-30T22:57:32Z is mapped to 2023-05-01T00:00:00Z
                // hence all the photos taken on 04.30 will be returned.

                // When using the redundancy workaround, the post filtering is needed.
                // See below.
                val redundantBefore =
                    if (timeToNextDay != DAY_MS)
                        Date(config.before.time + timeToNextDay)
                    else
                        config.before
                queryBuilder.append(" before:\"${formatPhotoPrismDate(redundantBefore)}\"")
            }

            queryBuilder.append(" public:${!config.includePrivate}")

            if (config.albumUid != null) {
                queryBuilder.append(" album:${config.albumUid}")
            }

            val params = Params(
                query = queryBuilder.toString()
                    .trim()
                    .takeUnless(String::isNullOrBlank),
                postFilterBefore = config.before,
            )

            return get(params)
        }

        fun get(params: Params = Params()): SimpleGalleryMediaRepository {
            val key = params.toString()

            return cache[key]
                ?: SimpleGalleryMediaRepository(
                    photoPrismPhotosService = photoPrismPhotosService,
                    thumbnailUrlFactory = thumbnailUrlFactory,
                    downloadUrlFactory = downloadUrlFactory,
                    webUrlFactory = webUrlFactory,
                    params = params,
                    pageLimit = pageLimit,
                ).also {
                    cache.put(key, it)
                }
        }

        /**
         * Invalidates all the cached repositories.
         */
        fun invalidateAllCached() {
            cache.snapshot().values.onEach(Repository::invalidate)
        }

        private companion object {
            private const val DAY_MS = 86400000L
        }
    }
}
