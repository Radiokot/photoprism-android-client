package ua.com.radiokot.photoprism.features.gallery.view.model

import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

interface GalleryMediaRemoteActionsViewModelDelegate : GalleryMediaRemoteActionsViewModel {

    fun deleteGalleryMedia(
        mediaUids: Collection<String>,
        currentMediaRepository: SimpleGalleryMediaRepository,
        onStarted: () -> Unit = {},
    )

    fun archiveGalleryMedia(
        mediaUids: Collection<String>,
        currentMediaRepository: SimpleGalleryMediaRepository,
        onStarted: () -> Unit = {},
    )

    fun addGalleryMediaToAlbum(
        mediaUids: Collection<String>,
        onStarted: () -> Unit = {},
    )

    fun updateGalleryMediaAttributes(
        mediaUid: String,
        currentMediaRepository: SimpleGalleryMediaRepository,
        isFavorite: Boolean? = null,
        isPrivate: Boolean? = null,
        onStarted: () -> Unit = {},
        onUpdated: () -> Unit = {},
    )
}
