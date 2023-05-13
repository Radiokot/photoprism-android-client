package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.albums.service.PhotoPrismAlbumsService
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.util.PagedCollectionLoader

/**
 * A repository for albums which the gallery content can be filtered by.
 * Combines albums of multiple [types].
 *
 * @param types types of albums to combine in the desired order, e.g. ["albums", "folders"]
 * @param comparator used to sort collected albums of given [types]
 */
class AlbumsRepository(
    private val photoPrismAlbumsService: PhotoPrismAlbumsService,
    private val previewUrlFactory: MediaPreviewUrlFactory,
    private val types: Collection<String>,
    private val comparator: Comparator<Album>?,
) : SimpleCollectionRepository<Album>() {
    override fun getCollection(): Single<List<Album>> =
        Single.mergeDelayError(types.map(::getAlbumsOfType))
            .collect<MutableList<Album>>(
                { mutableListOf() },
                { collectedAlbums, albums -> collectedAlbums.addAll(albums) }
            )
            .map { collectedAlbums ->
                if (comparator != null) {
                    collectedAlbums.sortWith(comparator)
                }

                collectedAlbums as List<Album>
            }

    /**
     * @return [Album] found by [uid] in the [itemsList]
     * or null if nothing found.
     */
    fun getLoadedAlbum(uid: String): Album? =
        itemsList.find { it.uid == uid }

    private fun getAlbumsOfType(type: String): Single<List<Album>> {
        val loader = PagedCollectionLoader(
            pageProvider = { cursor ->
                {
                    val offset = cursor?.toInt() ?: 0

                    val items = photoPrismAlbumsService.getAlbums(
                        count = PAGE_LIMIT,
                        offset = offset,
                        order = PhotoPrismOrder.FAVORITES,
                        type = type,
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