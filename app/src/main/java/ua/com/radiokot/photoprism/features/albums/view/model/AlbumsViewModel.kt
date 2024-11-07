package ua.com.radiokot.photoprism.features.albums.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsPreferences
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModel
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModelImpl

class AlbumsViewModel(
    private val albumsRepository: AlbumsRepository,
    private val preferences: AlbumsPreferences,
    private val searchPredicate: (album: Album, query: String) -> Boolean,
) : ViewModel(),
    SearchViewViewModel by SearchViewViewModelImpl() {

    private val log = kLogger("AlbumsVM")
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<AlbumListItem>>()
    val mainError = MutableLiveData<Error?>(null)
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPressed()
    }
    private var isInitialized = false
    lateinit var albumType: Album.TypeName
        private set
    private lateinit var defaultSearchConfig: SearchConfig
    private lateinit var sortPreferenceSubject: BehaviorSubject<AlbumSort>

    fun initOnce(
        albumType: Album.TypeName,
        defaultSearchConfig: SearchConfig,
    ) {
        if (isInitialized) {
            return
        }

        this.albumType = albumType
        this.defaultSearchConfig = defaultSearchConfig

        this.sortPreferenceSubject = when (albumType) {
            Album.TypeName.FOLDER ->
                preferences.folderSort

            else ->
                preferences.albumSort
        }

        subscribeToRepository()
        subscribeToSearch()
        subscribeToPreferences()

        update()

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\nalbumType=$albumType"
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

    private fun subscribeToPreferences() {
        sortPreferenceSubject
            .observeOnMain()
            .skip(1)
            .subscribe { postAlbumItems() }
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
            .subscribe { postAlbumItems() }
            .autoDispose(this)
    }

    private fun postAlbumItems() {
        val repositoryAlbums = albumsRepository.itemsList
            .filter { album ->
                album.type == albumType
            }
        val searchQuery = currentSearchInput
        val filteredRepositoryAlbums =
            if (searchQuery != null)
                repositoryAlbums.filter { album ->
                    searchPredicate(album, searchQuery)
                }
            else
                repositoryAlbums
        val sort: AlbumSort = sortPreferenceSubject.value!!

        log.debug {
            "postAlbumItems(): posting_items:" +
                    "\nalbumCount=${repositoryAlbums.size}," +
                    "\nsearchQuery=$searchQuery," +
                    "\nfilteredAlbumCount=${filteredRepositoryAlbums.size}," +
                    "\nsort=$sort"
        }

        itemsList.value = filteredRepositoryAlbums
            .sortedWith(sort)
            .map(::AlbumListItem)

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

    fun onAlbumItemClicked(item: AlbumListItem) {
        log.debug {
            "onAlbumItemClicked(): album_item_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            val uid = item.source.uid

            log.debug {
                "onAlbumItemClicked(): opening_album:" +
                        "\nuid=$uid"
            }

            eventsSubject.onNext(
                Event.OpenAlbum(
                    title = item.source.title,
                    repositoryParams = SimpleGalleryMediaRepository.Params(
                        searchConfig = defaultSearchConfig.copy(
                            includePrivate = true,
                            albumUid = uid,
                        )
                    )
                )
            )
        }
    }

    fun onSortClicked() {
        log.debug {
            "onSortClicked(): opening_sort_dialog"
        }

        eventsSubject.onNext(
            Event.OpenSortDialog(
                currentSort = sortPreferenceSubject.value!!,
            )
        )
    }

    fun onSortDialogResult(
        newSort: AlbumSort,
    ) {
        log.debug {
            "onSortDialogResult(): saving_new_sort" +
                    "\nnewSort=$newSort"
        }

        sortPreferenceSubject.onNext(newSort)
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

        class OpenAlbum(
            val title: String,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
        ) : Event

        /**
         * Show the sort dialog, return the result to [onSortDialogResult].
         */
        class OpenSortDialog(
            val currentSort: AlbumSort,
        ) : Event
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
