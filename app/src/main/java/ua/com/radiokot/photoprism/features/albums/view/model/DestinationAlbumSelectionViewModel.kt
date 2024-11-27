package ua.com.radiokot.photoprism.features.albums.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.albums.data.model.DestinationAlbum
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsPreferences
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchInputViewModel
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchInputViewModelImpl

class DestinationAlbumSelectionViewModel(
    private val albumsRepository: AlbumsRepository,
    private val preferences: AlbumsPreferences,
    private val searchPredicate: (album: DestinationAlbum, query: String) -> Boolean,
    private val exactMatchPredicate: (album: DestinationAlbum, query: String) -> Boolean,
) : ViewModel(),
    SearchInputViewModel by SearchInputViewModelImpl() {

    private val log = kLogger("DestinationAlbumSelectionVM")

    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<DestinationAlbumListItem>>()
    val mainError = MutableLiveData<Error?>(null)
    val isDoneButtonVisible = MutableLiveData(false)
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPressed()
    }
    private var isInitialized = false
    private var albumsToCreate: Set<DestinationAlbum.ToCreate> = emptySet()
    private var selectedAlbums: Set<DestinationAlbum> = emptySet()
    private var initiallySelectedAlbums: Set<DestinationAlbum> = emptySet()
    private var isSingleSelection = false

    init {
        subscribeToRepository()
        subscribeToSearch()

        update()
    }

    fun initOnce(
        currentlySelectedAlbums: Set<DestinationAlbum>,
        isSingleSelection: Boolean,
    ) {
        if (isInitialized) {
            return
        }

        initiallySelectedAlbums = currentlySelectedAlbums
        selectedAlbums = currentlySelectedAlbums
        albumsToCreate =
            currentlySelectedAlbums.filterIsInstance<DestinationAlbum.ToCreate>().toSet()
        this.isSingleSelection = isSingleSelection

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\ncurrentlySelectedAlbums:${currentlySelectedAlbums.size}," +
                    "\nisSingleSelection=$isSingleSelection"
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
        val albumSort = preferences.albumSort.value!!
        val allAlbums = albumsToCreate +
                albumsRepository.itemsList
                    .sortedWith(albumSort)
                    .map(DestinationAlbum::Existing)
        val searchQuery = currentSearchInput
        val filteredAlbums =
            if (searchQuery != null)
                allAlbums.filter { album ->
                    searchPredicate(album, searchQuery)
                }
            else
                allAlbums

        val newItems: MutableList<DestinationAlbumListItem> = filteredAlbums
            .map { album ->
                DestinationAlbumListItem.Album(
                    source = album,
                    isAlbumSelected = album in selectedAlbums,
                )
            }
            .toMutableList()

        // Only show "Create new" option if no exact match found.
        if (searchQuery != null && allAlbums.none { exactMatchPredicate(it, searchQuery) }) {
            newItems.add(
                0,
                DestinationAlbumListItem.CreateNew(
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

    fun onListItemClicked(item: DestinationAlbumListItem) {
        when (item) {
            is DestinationAlbumListItem.Album -> {
                if (item.source != null) {
                    switchAlbumSelection(
                        album = item.source,
                    )
                }
            }

            is DestinationAlbumListItem.CreateNew -> {
                addAlbumToCreate(
                    newAlbumTitle = item.newAlbumTitle,
                )
            }
        }
    }

    private fun addAlbumToCreate(newAlbumTitle: String) {
        val albumToCreate = DestinationAlbum.ToCreate(
            title = newAlbumTitle,
        )

        log.debug {
            "addAlbumToCreate(): adding:" +
                    "\nalbumToCreate=$albumToCreate"
        }

        albumsToCreate += albumToCreate

        switchAlbumSelection(albumToCreate)
    }

    private fun switchAlbumSelection(album: DestinationAlbum) {
        if (album in selectedAlbums) {
            log.debug {
                "switchAlbumSelection(): unselect:" +
                        "\nalbum=$album"
            }

            selectedAlbums -= album
        } else if (isSingleSelection) {
            log.debug {
                "switchAlbumSelection(): select_single:" +
                        "\nalbum=$album"
            }

            selectedAlbums = setOf(album)
        } else {
            log.debug {
                "switchAlbumSelection(): select:" +
                        "\nalbum=$album"
            }

            selectedAlbums += album
        }

        isDoneButtonVisible.value = selectedAlbums != initiallySelectedAlbums

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

    fun onDoneClicked() {
        log.debug {
            "onDoneClicked(): finishing_with_result"
        }

        eventsSubject.onNext(Event.FinishWithResult(selectedAlbums))
    }

    fun onSearchSubmit() {
        val allItems = itemsList.value
            ?: return

        val createNewItem = allItems
            .filterIsInstance<DestinationAlbumListItem.CreateNew>()
            .firstOrNull()
        val firstAlbumItem = allItems
            .filterIsInstance<DestinationAlbumListItem.Album>()
            .firstOrNull()

        if (createNewItem != null) {
            addAlbumToCreate(
                newAlbumTitle = createNewItem.newAlbumTitle,
            )

            rawSearchInput.value = ""
        } else if (firstAlbumItem?.source != null) {
            switchAlbumSelection(firstAlbumItem.source)

            rawSearchInput.value = ""
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

        /**
         * Set an OK result with the [selectedAlbums] and finish.
         */
        class FinishWithResult(
            val selectedAlbums: Set<DestinationAlbum>,
        ) : Event
    }

    sealed interface Error {
        /**
         * The loading is failed and could be retried.
         * The [onRetryClicked] method should be called.
         */
        object LoadingFailed : Error
    }
}
