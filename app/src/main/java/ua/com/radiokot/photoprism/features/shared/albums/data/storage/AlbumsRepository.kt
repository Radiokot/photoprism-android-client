package ua.com.radiokot.photoprism.features.shared.albums.data.storage

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.albums.service.PhotoPrismAlbumsService
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album
import ua.com.radiokot.photoprism.util.PagedCollectionLoader

/**
 * A repository for albums which the gallery content can be filtered by.
 * Combines albums of multiple types.
 */
class AlbumsRepository(
    private val photoPrismAlbumsService: PhotoPrismAlbumsService,
    private val types: Set<Album.TypeName>,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) : SimpleCollectionRepository<Album>() {
    private val comparator: Comparator<Album> =
        compareByDescending(Album::isFavorite)
            .thenBy(Album::title)

    override fun getCollection(): Single<List<Album>> =
        Single.mergeDelayError(types.map(::getAlbumsOfType))
            .collect<MutableList<Album>>(
                { mutableListOf() },
                { collectedAlbums, albums -> collectedAlbums.addAll(albums) }
            )
            .map { collectedAlbums ->
                collectedAlbums.sortedWith(comparator)
            }

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
                        order = PhotoPrismOrder.FAVORITES,
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
                photoPrismAlbums.map { photoPrismAlbum ->
                    Album(
                        source = photoPrismAlbum,
                        previewUrlFactory = previewUrlFactory,
                    )
                }
            }
            .subscribeOn(Schedulers.io())
    }

    private companion object {
        private const val PAGE_LIMIT = 30
        private const val ALBUM_TYPE = "album"
        private const val FOLDER_TYPE = "folder"
    }
}
