package ua.com.radiokot.photoprism.features.gallery.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class RemoveGalleryMediaFromAlbumUseCase(
    private val albumsRepository: AlbumsRepository,
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
) {

    /**
     * Removes given [mediaUids] from the album with the corresponding [albumUid].
     *
     * On success, removes the media from the album-related media repository, if present.
     * Albums repository gets invalidated to refresh thumbnails.
     */
    operator fun invoke(
        mediaUids: Collection<String>,
        albumUid: String,
    ): Completable =
        removeFromAlbum(
            albumUid = albumUid,
            mediaUids = mediaUids,
        )
            .doOnComplete {
                galleryMediaRepositoryFactory
                    .getCreated(
                        params = SimpleGalleryMediaRepository.Params(
                            searchConfig = SearchConfig.forAlbum(
                                albumUid = albumUid,
                            )
                        )
                    )
                    ?.removeLocally(mediaUids.toSet())
            }

    private fun removeFromAlbum(
        albumUid: String,
        mediaUids: Collection<String>,
    ): Completable =
        albumsRepository.removeItemsFromAlbum(
            albumUid = albumUid,
            itemUids = mediaUids,
        )
}
