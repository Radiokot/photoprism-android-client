package ua.com.radiokot.photoprism.features.albums.data.storage

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.albums.service.PhotoPrismAlbumsService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort
import ua.com.radiokot.photoprism.util.PagedCollectionLoader

/**
 * A repository for albums, which can be folders, actual albums, months, etc.
 * Combines albums of multiple [types] sorting them with [defaultSort].
 */
class AlbumsRepository(
    private val types: Set<Album.TypeName>,
    private val defaultSort: AlbumSort,
    private val photoPrismAlbumsService: PhotoPrismAlbumsService,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) : SimpleCollectionRepository<Album>() {
    override fun getCollection(): Single<List<Album>> =
        Single.mergeDelayError(types.map(::getAlbumsOfType))
            .collectInto(mutableListOf<Album>()) { collectedAlbums, albums ->
                collectedAlbums.addAll(albums)
            }
            .map { collectedAlbums ->
                // Collected (concat) albums must be sorted locally,
                // as PhotoPrism order is applied per collection.
                collectedAlbums.sortedWith(defaultSort)
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
    }
}
