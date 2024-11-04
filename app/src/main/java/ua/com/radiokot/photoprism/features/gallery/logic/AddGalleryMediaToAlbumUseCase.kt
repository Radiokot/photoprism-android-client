package ua.com.radiokot.photoprism.features.gallery.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.data.model.DestinationAlbum
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository

class AddGalleryMediaToAlbumUseCase(
    private val albumsRepository: AlbumsRepository,
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
