package ua.com.radiokot.photoprism.features.albums.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
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
import ua.com.radiokot.photoprism.features.gallery.data.storage.db.CachedAlbum
import ua.com.radiokot.photoprism.features.gallery.data.storage.db.CachedAlbumDao
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModel
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModelImpl
import ua.com.radiokot.photoprism.util.LocalDate

class AlbumsViewModel(
    private val albumsRepositoryFactory: AlbumsRepository.Factory,
    private val preferences: AlbumsPreferences,
    private val searchPredicate: (album: Album, query: String) -> Boolean,
    private val previewUrlFactory: MediaPreviewUrlFactory,
    private val cachedAlbumDao: CachedAlbumDao,
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
    private lateinit var albumsRepository: AlbumsRepository
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
        this.albumsRepository = albumsRepositoryFactory.forType(albumType)
        this.defaultSearchConfig = defaultSearchConfig

        this.sortPreferenceSubject = when (albumType) {
            Album.TypeName.FOLDER ->
                preferences.folderSort

            Album.TypeName.ALBUM ->
                preferences.albumSort

            Album.TypeName.MONTH ->
                preferences.monthSort
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
            .map { album ->
                AlbumListItem(
                    source = album,
                    previewUrlFactory = previewUrlFactory,
                    isCached = isAlbumCached(album.uid),
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

    private fun isAlbumCached(albumId: String): Observable<Boolean> {
        return cachedAlbumDao.isAlbumCached(albumId)
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

        val album = item.source
            ?: return

        log.debug {
            "onAlbumItemClicked(): opening_album:" +
                    "\nuid=${album.uid}"
        }

        eventsSubject.onNext(
            Event.OpenAlbum(
                title = album.title,
                monthTitle =
                if (album.type == Album.TypeName.MONTH)
                    album.ymdLocalDate
                else
                    null,
                albumUid =
                if (album.type == Album.TypeName.ALBUM)
                    album.uid
                else
                    null,
                repositoryParams = SimpleGalleryMediaRepository.Params(
                    searchConfig = SearchConfig.forAlbum(
                        albumUid = album.uid,
                        base = defaultSearchConfig,
                    )
                )
            )
        )
    }

    fun onCacheIconClicked(album: Album) {
        val albumId = album.uid

        val isCached = isAlbumCached(albumId).blockingFirst()

        val completable = if (isCached) {
            cachedAlbumDao.deleteCachedAlbum(albumId)
        } else {
            cachedAlbumDao.addCachedAlbum(CachedAlbum(albumId))
        }

        completable.subscribe().autoDispose(this)
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
            val monthTitle: LocalDate?,
            val albumUid: String?,
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
