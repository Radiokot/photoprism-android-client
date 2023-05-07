package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.storage.AlbumsRepository

/**
 * A viewmodel that controls list of selectable albums for the gallery search.
 *
 * @see updateIfNotFresh
 * @see selectedAlbumUid
 */
class GallerySearchAlbumsViewModel(
    private val albumsRepository: AlbumsRepository,
) : ViewModel() {
    private val log = kLogger("GallerySearchAlbumsVM")

    val state = MutableLiveData<State>(State.Loading)
    val selectedAlbumUid = MutableLiveData<String?>()

    init {
        subscribeToRepository()
        subscribeToAlbumSelection()
    }

    fun updateIfNotFresh() {
        log.debug {
            "updateIfNotFresh(): begin_loading"
        }

        albumsRepository.updateIfNotFresh()
    }

    private fun subscribeToRepository() {
        albumsRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                if (state.value is State.Loading) {
                    postReadyState()
                }
            }
            .addToCloseables(this)

        albumsRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { isLoading ->
                if (isLoading) {
                    state.value = State.Loading
                }
            }
            .addToCloseables(this)

        albumsRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.debug(error) {
                    "subscribeToRepository(): albums_loading_failed"
                }

                state.value = State.LoadingFailed
            }
            .addToCloseables(this)
    }

    private fun postReadyState() {
        val repositoryAlbums = albumsRepository.itemsList

        val selectedAlbumUid = selectedAlbumUid.value

        log.debug {
            "postReadyState(): posting_ready_state:" +
                    "\nalbumsCount=${repositoryAlbums.size}," +
                    "\nselectedAlbumUid=$selectedAlbumUid"
        }

        state.value = State.Ready(
            albums = repositoryAlbums.map { album ->
                AlbumListItem(
                    source = album,
                    isAlbumSelected = album.uid == selectedAlbumUid
                )
            }
        )
    }

    private fun subscribeToAlbumSelection() {
        selectedAlbumUid.observeForever { uid ->
            val currentState = state.value
            if (currentState is State.Ready) {
                postReadyState()
            }
        }
    }

    fun onAlbumItemClicked(item: AlbumListItem) {
        val currentState = state.value
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

    sealed interface State {
        object Loading : State
        class Ready(
            val albums: List<AlbumListItem>,
        ) : State

        object LoadingFailed : State
    }
}