package ua.com.radiokot.photoprism.features.importt.albums.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.PeopleOverviewViewModel.Event
import ua.com.radiokot.photoprism.features.importt.albums.data.model.ImportAlbum
import ua.com.radiokot.photoprism.view.model.search.SearchInputViewModel
import ua.com.radiokot.photoprism.view.model.search.SearchInputViewModelImpl

class ImportAlbumsViewModel(
    private val albumsRepository: AlbumsRepository,
    private val searchPredicate: (album: ImportAlbum, query: String) -> Boolean,
    private val exactMatchPredicate: (album: ImportAlbum, query: String) -> Boolean,
) : ViewModel(),
    SearchInputViewModel by SearchInputViewModelImpl() {

    private val log = kLogger("ImportAlbumsVM")

    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()
    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<ImportAlbumListItem>>()
    val mainError = MutableLiveData<Error?>(null)
    val isDoneButtonVisible = MutableLiveData(false)
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPressed()
    }
    private var isInitialized = false
    private var albumsToCreate: Set<ImportAlbum.ToCreate> = emptySet()
    private var selectedAlbums: Set<ImportAlbum> = emptySet()
    private var initiallySelectedAlbums: Set<ImportAlbum> = emptySet()

    init {
        subscribeToRepository()
        subscribeToSearch()

        update()
    }

    fun initOnce(
        currentlySelectedAlbums: Set<ImportAlbum>,
    ) {
        if (isInitialized) {
            return
        }

        initiallySelectedAlbums = currentlySelectedAlbums
        selectedAlbums = currentlySelectedAlbums
        albumsToCreate = currentlySelectedAlbums.filterIsInstance<ImportAlbum.ToCreate>().toSet()

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\ncurrentlySelectedAlbums:${currentlySelectedAlbums.size}"
        }
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

                if (albumsRepository.itemsList.isEmpty() && albumsRepository.isNeverUpdated) {
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
            .filter { !(albumsRepository.itemsList.isEmpty() && albumsRepository.isNeverUpdated) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postAlbumItems() }
            .autoDispose(this)
    }

    private fun postAlbumItems() {
        val allAlbums = albumsToCreate +
                albumsRepository.itemsList
                    .filter { it.type == Album.TypeName.ALBUM }
                    .map(ImportAlbum::Existing)
        val searchQuery = currentSearchInput
        val filteredAlbums =
            if (searchQuery != null)
                allAlbums.filter { album ->
                    searchPredicate(album, searchQuery)
                }
            else
                allAlbums

        val newItems: MutableList<ImportAlbumListItem> = filteredAlbums
            .map { album ->
                ImportAlbumListItem.Album(
                    source = album,
                    isAlbumSelected = album in selectedAlbums,
                )
            }
            .toMutableList()

        // Only show "Create new" option if no exact match found.
        if (searchQuery != null && allAlbums.none { exactMatchPredicate(it, searchQuery) }) {
            newItems.add(
                0,
                ImportAlbumListItem.CreateNew(
                    newAlbumTitle = searchQuery,
                )
            )
        }

        log.debug {
            "postAlbumItems(): posting_items:" +
                    "\nalbumCount=${allAlbums.size}," +
                    "\nselectedAlbumCount=${selectedAlbums.size}," +
                    "\nsearchQuery=$searchQuery," +
                    "\nfilteredAlbumCount=${filteredAlbums.size}," +
                    "\nnewItemCount=${newItems.size}"
        }

        itemsList.value = newItems
        mainError.value = null
    }

    fun onListItemClicked(item: ImportAlbumListItem) {
        when (item) {
            is ImportAlbumListItem.Album -> {
                if (item.source != null) {
                    switchAlbumSelection(
                        album = item.source,
                    )
                }
            }

            is ImportAlbumListItem.CreateNew -> {
                addAlbumToCreate(
                    newAlbumTitle = item.newAlbumTitle,
                )
            }
        }
    }

    private fun addAlbumToCreate(newAlbumTitle: String) {
        val albumToCreate = ImportAlbum.ToCreate(
            title = newAlbumTitle,
        )

        log.debug {
            "addAlbumToCreate(): adding:" +
                    "\nalbumToCreate=$albumToCreate"
        }

        albumsToCreate += albumToCreate

        postAlbumItems()
    }

    private fun switchAlbumSelection(album: ImportAlbum) {
        if (album in selectedAlbums) {
            log.debug {
                "switchAlbumSelection(): unselect:" +
                        "\nalbum=$album"
            }

            selectedAlbums -= album
        } else {
            log.debug {
                "switchAlbumSelection(): select:" +
                        "\nalbum=$album"
            }

            selectedAlbums += album
        }

        postAlbumItems()
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
        when {
            currentSearchInput != null -> {
                log.debug {
                    "onBackPressed(): clearing_search"
                }

                rawSearchInput.value = ""
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
    }
}
