package ua.com.radiokot.photoprism.features.gallery.data.storage

import androidx.collection.LruCache
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.model.PagingOrder
import ua.com.radiokot.photoprism.base.data.storage.SimplePagedDataRepository
import ua.com.radiokot.photoprism.extension.mapSuccessful
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

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
    override fun getPage(
        limit: Int,
        cursor: String?,
        order: PagingOrder
    ): Single<DataPage<GalleryMedia>> {
        val offset = cursor?.toInt() ?: 0

        return {


            photoPrismPhotosService.getPhotos(
                count = limit,
                offset = cursor?.toInt() ?: 0,
                q = query,
                order = when (pagingOrder) {
                    PagingOrder.DESC -> PhotoPrismOrder.NEWEST
                    PagingOrder.ASC -> PhotoPrismOrder.OLDEST
                }
            )
        }
            .toSingle()
            .subscribeOn(Schedulers.io())
            .map { photoPrismPhotos ->
                photoPrismPhotos.mapSuccessful {
                    GalleryMedia(
                        source = it,
                        previewUrlFactory = thumbnailUrlFactory,
                        downloadUrlFactory = downloadUrlFactory,
                    )
                }
            }
            .map { galleryMediaItems ->
                DataPage(
                    items = galleryMediaItems,
                    nextCursor = (limit + offset).toString()
                )
            }
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

        fun getFiltered(
            mediaTypes: Set<GalleryMedia.TypeName> = emptySet(),
            userQuery: String? = null
        ): SimpleGalleryMediaRepository {
            val queryBuilder = StringBuilder()

            if (mediaTypes.isNotEmpty()) {
                queryBuilder.append(
                    " type:${
                        mediaTypes.joinToString("|") { it.value }
                    }"
                )
            }

            if (userQuery != null) {
                queryBuilder.append(" $userQuery")
            }

            val query = queryBuilder.toString()
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