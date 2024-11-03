package ua.com.radiokot.photoprism.features.gallery.search.albums.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.gallery.search.data.storage.SearchPreferences
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModel
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModelImpl

class GallerySearchAlbumSelectionViewModel(
    private val albumsRepository: AlbumsRepository,
    private val searchPredicate: (album: Album, query: String) -> Boolean,
    private val searchPreferences: SearchPreferences,
) : ViewModel(),
    SearchViewViewModel by SearchViewViewModelImpl() {

    private val log = kLogger("GallerySearchAlbumSelectionVM")

    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    val selectedAlbumUid = MutableLiveData<String?>()
    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<GallerySearchAlbumListItem>>()
    val mainError = MutableLiveData<Error?>(null)
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPressed()
    }

    init {
        subscribeToRepository()
        subscribeToSearch()
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

    private fun subscribeToSearch() {
        searchInputObservable
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
        val includeFolders = searchPreferences.showAlbumFolders.value == true
        val repositoryAlbums = albumsRepository.itemsList
            .filter { album ->
                album.type == Album.TypeName.ALBUM
                        || (includeFolders && album.type == Album.TypeName.FOLDER)
            }
        val searchQuery = currentSearchInput
        val filteredRepositoryAlbums =
            if (searchQuery != null)
                repositoryAlbums.filter { album ->
                    searchPredicate(album, searchQuery)
                }
            else
                repositoryAlbums
        val selectedAlbumUid = selectedAlbumUid.value

        log.debug {
            "postAlbumItems(): posting_items:" +
                    "\nalbumCount=${repositoryAlbums.size}," +
                    "\nincludeFolders=${includeFolders}," +
                    "\nselectedAlbumUid=$selectedAlbumUid," +
                    "\nsearchQuery=$searchQuery," +
                    "\nfilteredAlbumsCount=${filteredRepositoryAlbums.size}"
        }

        itemsList.value =
            filteredRepositoryAlbums.map { album ->
                GallerySearchAlbumListItem(
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

    fun onAlbumItemClicked(item: GallerySearchAlbumListItem) {
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
                "onAlbumItemClicked(): finishing_with_result"
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
            "onSwipeRefreshPulled(): force_updating"
        }

        update(force = true)
    }

    private fun onBackPressed() {
        log.debug {
            "onBackPressed(): handling_back_press"
        }

        when {
            isSearchExpanded.value == true -> {
                closeAndClearSearch()
            }

            else -> {
                log.debug {
                    "onBackPressed(): finishing_without_result"
                }

                eventsSubject.onNext(Event.Finish)
            }
        }
    }

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         * Retry is possible: the [onRetryClicked] method should be called.
         */
        object ShowFloatingLoadingFailedError : Event

        /**
         * Set an OK result with the [selectedAlbumUid] and finish.
         */
        class FinishWithResult(
            val selectedAlbumUid: String?,
        ) : Event

        /**
         * Finish without setting a result.
         */
        object Finish : Event
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
