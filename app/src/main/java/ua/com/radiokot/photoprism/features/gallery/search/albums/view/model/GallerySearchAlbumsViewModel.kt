package ua.com.radiokot.photoprism.features.gallery.search.albums.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.search.data.storage.SearchPreferences

/**
 * A viewmodel that controls list of selectable albums for the gallery search.
 *
 * @see update
 * @see selectedAlbumUid
 */
class GallerySearchAlbumsViewModel(
    private val albumsRepository: AlbumsRepository,
    private val searchPreferences: SearchPreferences,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) : ViewModel() {
    private val log = kLogger("GallerySearchAlbumsVM")

    private val stateSubject = BehaviorSubject.createDefault<State>(State.Loading)
    val state = stateSubject.observeOnMain()
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    val selectedAlbumUid = MutableLiveData<String?>()
    val isViewVisible = searchPreferences.showAlbums.observeOnMain()

    init {
        subscribeToRepository()
        subscribeToPreferences()
        subscribeToAlbumSelection()
    }

    fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            albumsRepository.update()
        } else {
            albumsRepository.updateIfNotFresh()
        }
    }

    private fun subscribeToRepository() {
        albumsRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { albums ->
                if (albums.isEmpty() && albumsRepository.isNeverUpdated) {
                    stateSubject.onNext(State.Loading)
                } else {
                    postReadyState()
                }
            }
            .autoDispose(this)

        albumsRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepository(): albums_loading_failed"
                }

                stateSubject.onNext(State.LoadingFailed)
            }
            .autoDispose(this)
    }

    private fun subscribeToPreferences() {
        searchPreferences.showAlbumFolders
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (stateSubject.value is State.Ready) {
                    postReadyState()
                }
            }
            .autoDispose(this)
    }

    private fun postReadyState() {
        val includeFolders = searchPreferences.showAlbumFolders.value == true
        val selectedAlbumUid = selectedAlbumUid.value
        val repositoryAlbums = albumsRepository.itemsList
            .filter { album ->
                album.type == Album.TypeName.ALBUM
                        || (includeFolders && album.type == Album.TypeName.FOLDER)
            }

        log.debug {
            "postReadyState(): posting_ready_state:" +
                    "\nalbumCount=${repositoryAlbums.size}," +
                    "\nincludeFolders=$includeFolders," +
                    "\nselectedAlbumUid=$selectedAlbumUid"
        }

        stateSubject.onNext(
            State.Ready(
                albums = repositoryAlbums.map { album ->
                    GallerySearchAlbumListItem(
                        source = album,
                        isAlbumSelected = album.uid == selectedAlbumUid,
                        previewUrlFactory = previewUrlFactory,
                    )
                }
            ))
    }

    private fun subscribeToAlbumSelection() {
        selectedAlbumUid.observeForever {
            val currentState = stateSubject.value
            if (currentState is State.Ready) {
                postReadyState()
            }
        }
    }

    fun onAlbumItemClicked(item: GallerySearchAlbumListItem) {
        val currentState = stateSubject.value
        check(currentState is State.Ready) {
            "Albums are clickable only in the ready state"
        }

        log.debug {
            "onAlbumItemClicked(): album_item_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            val uid = item.source.uid

            val newSelectedAlbumUid: String? =
                if (selectedAlbumUid.value != uid)
                    uid
                else
                    null

            log.debug {
                "onAlbumItemClicked(): setting_selected_album_uid:" +
                        "\nnewUid=$newSelectedAlbumUid"
            }

            selectedAlbumUid.value = newSelectedAlbumUid
        }
    }

    fun onReloadAlbumsClicked() {
        log.debug {
            "onReloadAlbumsClicked(): reload_albums_clicked"
        }

        update()
    }

    fun onSeeAllClicked() {
        log.debug {
            "onSeeAllClicked(): opening_overview"
        }

        eventsSubject.onNext(
            Event.OpenAlbumSelectionForResult(
                selectedAlbumUid = selectedAlbumUid.value,
            )
        )
    }

    fun onAlbumSelectionResult(newSelectedAlbumUid: String?) {
        log.debug {
            "onAlbumSelectionResult(): setting_selected_album_uid:" +
                    "\nnewUid=$newSelectedAlbumUid"
        }

        selectedAlbumUid.value = newSelectedAlbumUid

        // If something is selected, ensure that it is visible.
        val currentState = stateSubject.value
        if (newSelectedAlbumUid != null && currentState is State.Ready) {
            val newSelectedAlbumIndex =
                currentState.albums
                    .indexOfFirst { it.source?.uid == newSelectedAlbumUid }

            if (newSelectedAlbumIndex != -1) {
                log.debug {
                    "onAlbumSelectionResult(): ensure_new_selected_item_visible:" +
                            "\nnewSelectedAlbumIndex=$newSelectedAlbumIndex"
                }

                eventsSubject.onNext(Event.EnsureListItemVisible(newSelectedAlbumIndex))
            }
        }
    }

    fun getAlbumTitle(uid: String): String? =
        albumsRepository.getLoadedAlbum(uid)?.title

    sealed interface State {
        object Loading : State
        class Ready(
            val albums: List<GallerySearchAlbumListItem>,
        ) : State

        object LoadingFailed : State
    }

    sealed interface Event {
        /**
         * Open album selection to get the result.
         *
         * [onAlbumSelectionResult] must be called when the result is obtained.
         */
        class OpenAlbumSelectionForResult(
            val selectedAlbumUid: String?,
        ) : Event

        /**
         * Ensure that the given item of the item list is visible on the screen.
         */
        class EnsureListItemVisible(val listItemIndex: Int) : Event
    }
}
