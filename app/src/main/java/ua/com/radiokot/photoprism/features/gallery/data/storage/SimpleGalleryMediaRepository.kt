package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.os.Parcelable
import androidx.collection.LruCache
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismBatchPhotoUids
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismPhotoUpdate
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.model.PagingOrder
import ua.com.radiokot.photoprism.base.data.storage.Repository
import ua.com.radiokot.photoprism.base.data.storage.SimplePagedDataRepository
import ua.com.radiokot.photoprism.env.data.model.WebPageInteractionRequiredException
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.mapSuccessful
import ua.com.radiokot.photoprism.extension.toMaybe
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.model.parsePhotoPrismDate
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import ua.com.radiokot.photoprism.util.LocalDate

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

                photoPrismPhotos
                    .mapSuccessful {
                        GalleryMedia(
                            source = it,
                            previewUrlFactory = thumbnailUrlFactory,
                            downloadUrlFactory = downloadUrlFactory,
                            webUrlFactory = webUrlFactory,
                        )
                    }
                    .filter { entry ->
                        // Precise post filter by "before" and "after" dates,
                        // workaround for PhotoPrism filtering.
                        (params.postFilterBefore == null || entry.takenAtLocal < params.postFilterBefore)
                                && (params.postFilterAfter == null || entry.takenAtLocal > params.postFilterAfter)
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
            .onErrorResumeNext { error ->
                Completable.error(
                    if (WebPageInteractionRequiredException.THROWABLE_PREDICATE.test(error))
                        WebPageInteractionRequiredException()
                    else
                        error
                )
            }
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

    private var newestAndOldestDates: Pair<LocalDate, LocalDate>? = null
    fun getNewestAndOldestLocalDates(): Maybe<Pair<LocalDate, LocalDate>> {
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
                ?.let(::LocalDate)
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
                ?.let(::LocalDate)
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

    fun updateAttributes(
        itemUid: String,
        isFavorite: Boolean? = null,
        isPrivate: Boolean? = null,
    ): Completable = {
        photoPrismPhotosService.updatePhoto(
            photoUid = itemUid,
            update = PhotoPrismPhotoUpdate(
                favorite = isFavorite,
                private = isPrivate,
            ),
        )
    }
        .toCompletable()
        .doOnComplete {
            itemsList
                .find { it.uid == itemUid }
                ?.also { itemToChange ->
                    // Update the state locally.
                    isFavorite?.also(itemToChange::isFavorite::set)
                    isPrivate?.also(itemToChange::isPrivate::set)

                    broadcast()
                }
        }

    fun archive(
        itemUids: Collection<String>
    ): Completable = {
        photoPrismPhotosService.batchArchive(PhotoPrismBatchPhotoUids(itemUids))
        removeItems(itemUids.toSet())
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            broadcast()
        }

    fun delete(
        itemUids: Collection<String>
    ): Completable = {
        photoPrismPhotosService.batchDelete(PhotoPrismBatchPhotoUids(itemUids))
        removeItems(itemUids.toSet())
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            broadcast()
        }

    private fun removeItems(itemUids: Set<String>) = synchronized(this) {
        mutableItemsList.removeAll { it.uid in itemUids }
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
     * @param postFilterBefore local time to apply post filtering of the items,
     * as PhotoPrism is incapable of precise local time filtering.
     * @param postFilterAfter local time to apply post filtering of the items,
     * as PhotoPrism is incapable of precise local time filtering.
     */
    @Parcelize
    data class Params(
        val query: String? = null,
        val postFilterBefore: LocalDate? = null,
        val postFilterAfter: LocalDate? = null,
    ) : Parcelable {

        constructor(searchConfig: SearchConfig) : this(
            query = searchConfig.getPhotoPrismQuery(),
            postFilterBefore = searchConfig.beforeLocal,
            postFilterAfter = searchConfig.afterLocal,
        )
    }

    class Factory(
        private val photoPrismPhotosService: PhotoPrismPhotosService,
        private val thumbnailUrlFactory: MediaPreviewUrlFactory,
        private val downloadUrlFactory: MediaFileDownloadUrlFactory,
        private val webUrlFactory: MediaWebUrlFactory,
        private val defaultPageLimit: Int,
    ) {
        private val cache = LruCache<String, SimpleGalleryMediaRepository>(10)

        fun get(searchConfig: SearchConfig) =
            get(Params(searchConfig))

        fun get(params: Params = Params()): SimpleGalleryMediaRepository {
            val key = params.toString()

            return cache[key]
                ?: create(params).also {
                    cache.put(key, it)
                }
        }

        fun create(
            params: Params = Params(),
            pageLimit: Int = defaultPageLimit,
        ) = SimpleGalleryMediaRepository(
            photoPrismPhotosService = photoPrismPhotosService,
            thumbnailUrlFactory = thumbnailUrlFactory,
            downloadUrlFactory = downloadUrlFactory,
            webUrlFactory = webUrlFactory,
            params = params,
            pageLimit = pageLimit,
        )

        /**
         * Invalidates all the cached repositories.
         */
        fun invalidateAllCached() {
            cache.snapshot().values.onEach(Repository::invalidate)
        }
    }
}
