package ua.com.radiokot.photoprism.features.gallery.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.data.model.DestinationAlbum
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

class AddGalleryMediaToAlbumUseCase(
    private val albumsRepository: AlbumsRepository,
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
) {
    fun invoke(
        mediaUids: Collection<String>,
        destinationAlbum: DestinationAlbum,
    ): Completable =
        getAlbumUid(destinationAlbum)
            .flatMapCompletable { albumUid ->
                addToAlbum(
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
                            ?.invalidate()
                    }
            }

    private fun getAlbumUid(destinationAlbum: DestinationAlbum): Single<String> =
        when (destinationAlbum) {
            is DestinationAlbum.Existing ->
                Single.just(destinationAlbum.uid)

            is DestinationAlbum.ToCreate ->
                albumsRepository.create(
                    title = destinationAlbum.title,
                )
                    .map(Album::uid)
        }

    private fun addToAlbum(
        albumUid: String,
        mediaUids: Collection<String>,
    ): Completable =
        albumsRepository.addItemsToAlbum(
            albumUid = albumUid,
            itemUids = mediaUids,
        )
}
