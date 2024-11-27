package ua.com.radiokot.photoprism.features.albums.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbumCreation
import ua.com.radiokot.photoprism.api.albums.service.PhotoPrismAlbumsService
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismBatchPhotoUids
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.util.PagedCollectionLoader

/**
 * A repository for albums, which can be folders, actual albums, months, etc.
 * Combines albums of multiple [types].
 */
class AlbumsRepository(
    private val types: Set<Album.TypeName>,
    private val photoPrismAlbumsService: PhotoPrismAlbumsService,
) : SimpleCollectionRepository<Album>() {
    override fun getCollection(): Single<List<Album>> =
        Single.mergeDelayError(types.map(::getAlbumsOfType))
            .collectInto(mutableListOf<Album>()) { collectedAlbums, albums ->
                collectedAlbums.addAll(albums)
            }
            .map(MutableList<Album>::toList)

    /**
     * @return [Album] found by [uid] in the [itemsList]
     * or null if nothing found.
     */
    fun getLoadedAlbum(uid: String): Album? =
        itemsList.find { it.uid == uid }

    private fun getAlbumsOfType(type: Album.TypeName): Single<List<Album>> {
        val loader = PagedCollectionLoader(
            pageProvider = { cursor ->
                {
                    val offset = cursor?.toInt() ?: 0

                    val items = photoPrismAlbumsService.getAlbums(
                        count = PAGE_LIMIT,
                        offset = offset,
                        type = type.value,
                    )

                    DataPage(
                        items = items,
                        nextCursor = (PAGE_LIMIT + offset).toString(),
                        isLast = items.size < PAGE_LIMIT,
                    )
                }.toSingle()
            }
        )

        return loader
            .loadAll()
            .map { photoPrismAlbums ->
                photoPrismAlbums.map(::Album)
            }
            .subscribeOn(Schedulers.io())
    }

    fun create(
        title: String,
    ): Single<Album> = {
        photoPrismAlbumsService.createAlbum(
            PhotoPrismAlbumCreation(
                title = title,
            )
        )
            .let(::Album)
            .also { createdAlbum ->
                mutableItemsList.add(createdAlbum)
                broadcast()
            }
    }.toSingle().subscribeOn(Schedulers.io())

    fun addItemsToAlbum(
        albumUid: String,
        itemUids: Collection<String>,
    ): Completable = {
        photoPrismAlbumsService.addPhotos(
            albumUid = albumUid,
            batchPhotoUids = PhotoPrismBatchPhotoUids(itemUids),
        )

        // Invalidate as adding items may change the album thumb,
        // but unfortunately it is not updated instantly.
        invalidate()
    }.toCompletable().subscribeOn(Schedulers.io())

    class Factory(
        private val photoPrismAlbumsService: PhotoPrismAlbumsService,
    ) {
        val albums: AlbumsRepository by lazy {
            AlbumsRepository(
                types = setOf(Album.TypeName.ALBUM),
                photoPrismAlbumsService = photoPrismAlbumsService,
            )
        }

        val folders: AlbumsRepository by lazy {
            AlbumsRepository(
                types = setOf(Album.TypeName.FOLDER),
                photoPrismAlbumsService = photoPrismAlbumsService,
            )
        }

        val months: AlbumsRepository by lazy {
            AlbumsRepository(
                types = setOf(Album.TypeName.MONTH),
                photoPrismAlbumsService = photoPrismAlbumsService,
            )
        }

        fun forType(type: Album.TypeName): AlbumsRepository = when (type) {
            Album.TypeName.ALBUM ->
                albums

            Album.TypeName.FOLDER ->
                folders

            Album.TypeName.MONTH ->
                months
        }
    }

    private companion object {
        private const val PAGE_LIMIT = 30
    }
}
