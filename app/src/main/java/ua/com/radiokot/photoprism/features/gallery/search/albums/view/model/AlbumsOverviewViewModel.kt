package ua.com.radiokot.photoprism.features.gallery.search.albums.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
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

    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()
    val selectedAlbumUid = MutableLiveData<String?>()
    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<AlbumListItem>>()
    val mainError = MutableLiveData<Error?>(null)

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
            .filter { !albumsRepository.isNeverUpdated }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postAlbumItems() }
            .autoDispose(this)

        albumsRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepository(): albums_loading_failed"
                }

                if (itemsList.value == null) {
                    mainError.value = Error.LoadingFailed
                } else {
                    eventsSubject.onNext(Event.ShowFloatingLoadingFailedError)
                }
            }
            .autoDispose(this)

        albumsRepository.loading
            .subscribe(isLoading::postValue)
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
            // Only react to the albums are loaded.
            .filter { itemsList.value != null }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postAlbumItems() }
            .autoDispose(this)
    }

    private fun subscribeToAlbumSelection() {
        selectedAlbumUid.observeForever {
            if (itemsList.value != null) {
                postAlbumItems()
            }
        }
    }

    private fun postAlbumItems() {
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
            "postAlbumItems(): posting_items:" +
                    "\nalbumsCount=${repositoryAlbums.size}," +
                    "\nselectedAlbumUid=$selectedAlbumUid," +
                    "\nfilter=$filter," +
                    "\nfilteredAlbumsCount=${filteredRepositoryAlbums.size}"
        }

        itemsList.value =
            filteredRepositoryAlbums.map { album ->
                AlbumListItem(
                    source = album,
                    isAlbumSelected = album.uid == selectedAlbumUid,
                )
            }

        mainError.value =
            when {
                filteredRepositoryAlbums.isEmpty() ->
                    Error.NothingFound

                else ->
                    // Dismiss the main error when there are items.
                    null
            }
    }

    fun onAlbumItemClicked(item: AlbumListItem) {
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

            log.debug {
                "onAlbumItemClicked(): finishing"
            }

            eventsSubject.onNext(Event.FinishWithResult(newSelectedAlbumUid))
        }
    }

    fun onRetryClicked() {
        log.debug {
            "onRetryClicked(): updating"
        }

        update(force = true)
    }

    fun onSwipeRefreshPulled() {
        log.debug {
            "onRetryClicked(): force_updating"
        }

        update(force = true)
    }

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         * Retry is possible: the [onRetryClicked] method should be called.
         */
        object ShowFloatingLoadingFailedError : Event

        class FinishWithResult(
            val selectedAlbumUid: String?,
        ) : Event
    }

    sealed interface Error {
        /**
         * The loading is failed and could be retried.
         * The [onRetryClicked] method should be called.
         */
        object LoadingFailed : Error

        /**
         * No albums found for the filter or there are simply no albums.
         */
        object NothingFound : Error
    }
}
