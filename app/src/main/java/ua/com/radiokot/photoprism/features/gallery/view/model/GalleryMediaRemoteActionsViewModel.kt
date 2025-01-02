package ua.com.radiokot.photoprism.features.gallery.view.model

import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.features.albums.data.model.DestinationAlbum

interface GalleryMediaRemoteActionsViewModel {

    val galleryMediaRemoteActionsEvents: PublishSubject<Event>

    fun onDeletingGalleryMediaConfirmed()
    fun onAlbumForAddingGalleryMediaSelected(selectedAlbum: DestinationAlbum)

    sealed interface Event {
        /**
         * Show items deletion confirmation, reporting the choice
         * to the [onDeletingGalleryMediaConfirmed] method.
         */
        object OpenDeletingConfirmationDialog : Event

        /**
         * Open destination album selection screen, reporting the choice
         * to the [onAlbumForAddingGalleryMediaSelected] method.
         */
        object OpenAlbumForAddingSelection : Event

        class ShowFloatingAddedToAlbumMessage(
            val albumTitle: String,
        ) : Event
    }
}
