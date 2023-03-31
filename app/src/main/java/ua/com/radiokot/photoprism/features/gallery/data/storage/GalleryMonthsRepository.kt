package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.albums.service.PhotoPrismAlbumsService
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMonth
import ua.com.radiokot.photoprism.util.PagedCollectionLoader

class GalleryMonthsRepository(
    private val albumsService: PhotoPrismAlbumsService,
) : SimpleCollectionRepository<GalleryMonth>() {
    private var galleryMediaQuery: String? = null

    override fun getCollection(): Single<List<GalleryMonth>> =
        PagedCollectionLoader(
            pageProvider = { cursor ->
                {
                    val count = 50
                    val offset = cursor?.toInt() ?: 0
                    val albums = albumsService.getAlbums(
                        count = count,
                        offset = offset,
                        type = "month",
                        order = PhotoPrismOrder.OLDEST,
                        q = galleryMediaQuery,
                    )

                    DataPage(
                        items = albums,
                        nextCursor = (count + offset).toString(),
                        isLast = albums.size < count,
                    )
                }.toSingle().subscribeOn(Schedulers.io())
            },
            distinct = false,
        )
            .loadAll()
            .map { albums ->
                albums
                    .map(::GalleryMonth)
                    .sortedDescending()
            }

    /**
     * @param galleryMediaQuery query of the current gallery media search
     *
     * @see [SimpleGalleryMediaRepository.query]
     */
    fun update(galleryMediaQuery: String?): Single<List<GalleryMonth>> =
        updateDeferred()
            .toSingle { itemsList }
            .doOnSubscribe {
                this.galleryMediaQuery = galleryMediaQuery
                mutableItemsList.clear()
                broadcast()
            }
}