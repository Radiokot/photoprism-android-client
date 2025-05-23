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
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemsOrder
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.model.parsePhotoPrismDate
import ua.com.radiokot.photoprism.features.people.data.model.Person
import ua.com.radiokot.photoprism.util.LocalDate

/**
 * Turned out to be not that simple after all...
 */
class SimpleGalleryMediaRepository(
    private val photoPrismPhotosService: PhotoPrismPhotosService,
    val params: Params,
) : SimplePagedDataRepository<GalleryMedia>(
    pagingOrder = when (params.itemsOrder) {
        GalleryItemsOrder.NEWEST_FIRST ->
            PagingOrder.DESC

        GalleryItemsOrder.OLDEST_FIRST ->
            PagingOrder.ASC
    },
    pageLimit = params.pageLimit,
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
            ).asSequence()
        }
            .toSingle()
            .map { photoPrismPhotos ->
                val filesCount = photoPrismPhotos.sumOf { it.files.size }
                pageIsLast = filesCount < lookaheadLimit

                log.debug {
                    "getPage(): raw_page_loaded:" +
                            "\nfilesCount=$filesCount," +
                            "\npageIsLast=$pageIsLast"
                }

                photoPrismPhotos
                    .run {
                        // Filter by excluded persons before mapping
                        // as markers aren't needed for anything else.
                        val excludePersonIds = params.postFilterExcludePersonIds
                        if (excludePersonIds.isNotEmpty())
                            filterNot { photoPrismMergedPhoto ->
                                photoPrismMergedPhoto.files.any { file ->
                                    val markers = file.markers
                                        ?: return@any false
                                    markers.any { marker ->
                                        marker.faceId in excludePersonIds
                                                || marker.subjectUid in excludePersonIds
                                    }
                                }
                            }
                        else
                            this
                    }
                    .mapSuccessful(GalleryMedia::fromPhotoPrism)
                    .filter { entry ->
                        // Precise post filter by "before" and "after" dates,
                        // workaround for PhotoPrism filtering.
                        // Only filter dates after successfully parsing them above.
                        (params.postFilterBefore == null || entry.takenAtLocal < params.postFilterBefore)
                                && (params.postFilterAfter == null || entry.takenAtLocal > params.postFilterAfter)
                    }
                    .toList()
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
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            removeLocally(itemUids.toSet())
        }

    fun delete(
        itemUids: Collection<String>
    ): Completable = {
        photoPrismPhotosService.batchDelete(PhotoPrismBatchPhotoUids(itemUids))
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            removeLocally(itemUids.toSet())
        }

    fun removeLocally(itemUids: Set<String>) = synchronized(this) {
        mutableItemsList.removeAll { it.uid in itemUids }
        broadcast()
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
     * @param query PhotoPrism search query
     * @param postFilterBefore local time to apply post filtering of the items,
     * as PhotoPrism is incapable of precise local time filtering.
     * @param postFilterAfter local time to apply post filtering of the items,
     * as PhotoPrism is incapable of precise local time filtering.
     * @param postFilterExcludePersonIds a set of [Person.id] which may be subjects or faces,
     * to filter out items with any of them.
     * @param pageLimit target limit setting the minimum number of items in the page.
     * The actual pages are bigger due to the PhotoPrism pagination workaround.
     */
    @Parcelize
    data class Params(
        val query: String? = null,
        val postFilterBefore: LocalDate? = null,
        val postFilterAfter: LocalDate? = null,
        val postFilterExcludePersonIds: Set<String> = emptySet(),
        val pageLimit: Int = DEFAULT_PAGE_LIMIT,
        val itemsOrder: GalleryItemsOrder = GalleryItemsOrder.NEWEST_FIRST,
    ) : Parcelable {

        constructor(
            searchConfig: SearchConfig,
            postFilterExcludePersonIds: Set<String> = emptySet(),
            pageLimit: Int = DEFAULT_PAGE_LIMIT,
            itemsOrder: GalleryItemsOrder = GalleryItemsOrder.NEWEST_FIRST,
        ) : this(
            query = searchConfig.getPhotoPrismQuery(),
            postFilterBefore = searchConfig.beforeLocal,
            postFilterAfter = searchConfig.afterLocal,
            postFilterExcludePersonIds = postFilterExcludePersonIds,
            pageLimit = pageLimit,
            itemsOrder = itemsOrder,
        )

        companion object {
            // Always 80 elements – not great, not terrible.
            // It is better, of course, to dynamically adjust
            // to the max number of items on the screen.
            const val DEFAULT_PAGE_LIMIT = 80
        }
    }

    class Factory(
        private val photoPrismPhotosService: PhotoPrismPhotosService,
    ) {
        private val cache = LruCache<String, SimpleGalleryMediaRepository>(10)

        fun get(searchConfig: SearchConfig) =
            get(Params(searchConfig))

        fun get(
            params: Params = Params(),
        ): SimpleGalleryMediaRepository {
            val key = params.asKey()
            return cache[key]
                ?: create(params).also {
                    cache.put(key, it)
                }
        }

        fun getCreated(params: Params): SimpleGalleryMediaRepository? =
            cache[params.asKey()]

        fun create(
            params: Params = Params(),
        ) = SimpleGalleryMediaRepository(
            photoPrismPhotosService = photoPrismPhotosService,
            params = params,
        )

        /**
         * Invalidates all the cached repositories.
         */
        fun invalidateAllCached() {
            cache.snapshot().values.onEach(Repository::invalidate)
        }

        private fun Params.asKey(): String =
            toString()
    }
}
