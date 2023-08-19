package ua.com.radiokot.photoprism.features.gallery.search.albums.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.storage.AlbumsRepository
import java.util.concurrent.TimeUnit

class AlbumsOverviewViewModel(
    private val albumsRepository: AlbumsRepository,
    private val filterPredicate: (album: Album, filter: String) -> Boolean,
) : ViewModel() {
    private val log = kLogger("AlbumsOverviewVM")

    private val stateSubject = BehaviorSubject.createDefault<State>(State.Loading)
    val state = stateSubject.toMainThreadObservable()
    val selectedAlbumUid = MutableLiveData<String?>()

    /**
     * Raw input of the search view.
     */
    val filterInput = MutableLiveData("")

    init {
        subscribeToRepository()
        subscribeToFilter()
        subscribeToAlbumSelection()

        update()
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

    private fun subscribeToFilter() {
        Observable
            .create { emitter ->
                filterInput.observeForever(emitter::onNext)
            }
            .debounce { value ->
                // Apply debounce to the input unless it is empty (input is cleared).
                if (value.isEmpty())
                    Observable.just(0L)
                else
                    Observable.timer(400, TimeUnit.MILLISECONDS)
            }
            // Only react to the input in the ready state.
            .filter { stateSubject.value is State.Ready }
            .subscribe { postReadyState() }
            .autoDispose(this)
    }

    private fun subscribeToAlbumSelection() {
        selectedAlbumUid.observeForever {
            val currentState = stateSubject.value
            if (currentState is State.Ready) {
                postReadyState()
            }
        }
    }

    private fun postReadyState() {
        val repositoryAlbums = albumsRepository.itemsList
        val filter = filterInput.value?.takeIf(String::isNotEmpty)
        val filteredRepositoryAlbums =
            if (filter != null)
                repositoryAlbums.filter { album ->
                    filterPredicate(album, filter)
                }
            else
                repositoryAlbums
        val selectedAlbumUid = selectedAlbumUid.value

        log.debug {
            "postReadyState(): posting_ready_state:" +
                    "\nalbumsCount=${repositoryAlbums.size}," +
                    "\nselectedAlbumUid=$selectedAlbumUid," +
                    "\nfilter=$filter," +
                    "\nfilteredAlbumsCount=${filteredRepositoryAlbums.size}"
        }

        stateSubject.onNext(
            State.Ready(
                filter = filter,
                albums = filteredRepositoryAlbums.map { album ->
                    AlbumListItem(
                        source = album,
                        isAlbumSelected = album.uid == selectedAlbumUid,
                    )
                }
            ))
    }

    fun onAlbumItemClicked(item: AlbumListItem) {
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

    fun onRetryClicked() {
        log.debug {
            "onRetryClicked(): retry_clicked"
        }

        update()
    }

    sealed interface State {
        object Loading : State
        class Ready(
            val filter: String?,
            val albums: List<AlbumListItem>,
        ) : State

        object LoadingFailed : State
    }
}
