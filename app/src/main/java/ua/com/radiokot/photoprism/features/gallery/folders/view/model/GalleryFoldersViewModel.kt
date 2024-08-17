package ua.com.radiokot.photoprism.features.gallery.folders.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumSelectionViewModel.Error
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumSelectionViewModel.Event
import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album
import ua.com.radiokot.photoprism.features.shared.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModel
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModelImpl

class GalleryFoldersViewModel(
    private val albumsRepository: AlbumsRepository,
    private val searchPredicate: (album: Album, query: String) -> Boolean,
) : ViewModel(),
    SearchViewViewModel by SearchViewViewModelImpl() {

    private val log = kLogger("GalleryFoldersVM")
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()
    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<GalleryFolderListItem>>()
    val mainError = MutableLiveData<Error?>(null)
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPressed()
    }

    init {
        subscribeToRepository()
        subscribeToSearch()

        update()
    }

    private fun subscribeToRepository() {
        albumsRepository.items
            .filter { !albumsRepository.isNeverUpdated }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postFolderItems() }
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

    private fun update(force: Boolean = false) {
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

    private fun subscribeToSearch() {
        searchInputObservable
            // Only react to the folders are loaded.
            .filter { itemsList.value != null }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postFolderItems() }
            .autoDispose(this)

        albumsRepository.loading
            .subscribe(isLoading::postValue)
            .autoDispose(this)
    }

    private fun postFolderItems() {
        val repositoryAlbums = albumsRepository.itemsList
            .filter { album ->
                album.type == Album.TypeName.FOLDER
            }
        val searchQuery = currentSearchInput
        val filteredRepositoryAlbums =
            if (searchQuery != null)
                repositoryAlbums.filter { album ->
                    searchPredicate(album, searchQuery)
                }
            else
                repositoryAlbums

        log.debug {
            "postFolderItems(): posting_items:" +
                    "\nalbumCount=${repositoryAlbums.size}," +
                    "\nsearchQuery=$searchQuery," +
                    "\nfilteredAlbumCount=${filteredRepositoryAlbums.size}"
        }

        itemsList.value = filteredRepositoryAlbums.map(::GalleryFolderListItem)

        mainError.value =
            when {
                filteredRepositoryAlbums.isEmpty() ->
                    Error.NothingFound

                else ->
                    // Dismiss the main error when there are items.
                    null
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

        object Finish : Event
    }

    sealed interface Error {
        /**
         * The loading is failed and could be retried.
         * The [onRetryClicked] method should be called.
         */
        object LoadingFailed : Error

        /**
         * No folders found for the filter or there are simply no folders.
         */
        object NothingFound : Error
    }
}
