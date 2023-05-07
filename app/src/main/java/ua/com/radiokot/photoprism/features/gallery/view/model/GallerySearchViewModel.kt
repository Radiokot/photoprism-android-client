package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository

class GallerySearchViewModel(
    private val bookmarksRepository: SearchBookmarksRepository,
    val albumsViewModel: GallerySearchAlbumsViewModel,
    private val searchFiltersGuideUrl: String,
) : ViewModel() {
    private val log = kLogger("GallerySearchViewModel")

    private val defaultAvailableMediaTypes = setOf(
        GalleryMedia.TypeName.IMAGE,
        GalleryMedia.TypeName.VIDEO,
        GalleryMedia.TypeName.ANIMATED,
        GalleryMedia.TypeName.LIVE,
        GalleryMedia.TypeName.RAW,
        GalleryMedia.TypeName.VECTOR,
    )
    val areSomeTypesUnavailable = MutableLiveData(false)
    val availableMediaTypes = MutableLiveData(defaultAvailableMediaTypes).apply {
        observeForever { areSomeTypesUnavailable.value = it.size < defaultAvailableMediaTypes.size }
    }

    private val searchDefaults = SearchConfig.DEFAULT

    val isApplyButtonEnabled = MutableLiveData(false)
    private val stateSubject = BehaviorSubject.createDefault<State>(State.NoSearch)
    val state: Observable<State> = stateSubject
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject
    val isBookmarksSectionVisible = MutableLiveData(false)
    val bookmarks = MutableLiveData<List<SearchBookmarkItem>>()

    // Current search configuration values.
    // Values are set to searchDefaults values in onConfigurationViewOpening()
    val selectedMediaTypes = MutableLiveData<Set<GalleryMedia.TypeName>>()
    val userQuery = MutableLiveData<String>()
    val includePrivateContent = MutableLiveData<Boolean>()
    private val selectedAlbumUid = albumsViewModel.selectedAlbumUid

    init {
        val updateApplyButtonEnabled = { _: Any? ->
            isApplyButtonEnabled.postValue(canApplyConfiguredSearch)
        }

        selectedMediaTypes.observeForever(updateApplyButtonEnabled)
        userQuery.observeForever(updateApplyButtonEnabled)
        includePrivateContent.observeForever(updateApplyButtonEnabled)
        selectedAlbumUid.observeForever(updateApplyButtonEnabled)

        subscribeToBookmarks()

        // External data is updated on config view opening as well.
        // ::onConfigurationViewOpening.
        updateExternalData()
    }

    private val canApplyConfiguredSearch: Boolean
        get() {
            return selectedMediaTypes.value != searchDefaults.mediaTypes
                    || userQuery.value != searchDefaults.userQuery
                    || includePrivateContent.value != searchDefaults.includePrivate
                    || selectedAlbumUid.value != searchDefaults.albumUid
        }

    private val areBookmarksCurrentlyMoving = MutableLiveData(false)

    val canMoveBookmarks: Boolean
        get() = stateSubject.value is State.ConfiguringSearch
                && !bookmarksRepository.isLoading
                && areBookmarksCurrentlyMoving.value == false

    private fun subscribeToBookmarks() {
        bookmarksRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { bookmarks ->
                log.debug {
                    "subscribeToBookmarks(): received_new_bookmarks:" +
                            "\ncount=${bookmarks.size}"
                }

                onBookmarksUpdated(bookmarks)
            }
            .addToCloseables(this)
    }

    private fun onBookmarksUpdated(newBookmarks: List<SearchBookmark>) {
        this.bookmarks.value = newBookmarks.map(::SearchBookmarkItem)
        isBookmarksSectionVisible.value = newBookmarks.isNotEmpty()

        val currentState = stateSubject.value!!
        if (currentState is State.AppliedSearch) {
            val matchedBookmark = bookmarksRepository.findByConfig(currentState.search.config)
            stateSubject.onNext(
                State.AppliedSearch(
                    // If a bookmark has been created or updated for the current search,
                    // switch to an equivalent bookmarked search.
                    if (matchedBookmark != null)
                        AppliedGallerySearch.Bookmarked(matchedBookmark)
                    // If the current bookmark has been removed,
                    // switch to an equivalent custom search.
                    else
                        AppliedGallerySearch.Custom(currentState.search.config)
                )
            )
        }
    }

    fun onAvailableMediaTypeClicked(typeName: GalleryMedia.TypeName) {
        val currentlySelected = selectedMediaTypes.value ?: emptySet()
        if (currentlySelected.contains(typeName)) {
            log.debug {
                "onAvailableMediaTypeClicked(): unselect:" +
                        "\ntypeName=$typeName"
            }
            selectedMediaTypes.value = currentlySelected - typeName
        } else {
            log.debug {
                "onAvailableMediaTypeClicked(): select:" +
                        "\ntypeName=$typeName"
            }
            selectedMediaTypes.value = currentlySelected + typeName
        }
    }

    fun onConfigurationViewOpening() {
        log.debug {
            "onConfigurationViewOpening(): configuration_view_is_opening"
        }

        when (val state = stateSubject.value!!) {
            is State.AppliedSearch -> {
                selectedMediaTypes.value = state.search.config.mediaTypes
                userQuery.value = state.search.config.userQuery
                includePrivateContent.value = state.search.config.includePrivate
                selectedAlbumUid.value = state.search.config.albumUid

                stateSubject.onNext(
                    State.ConfiguringSearch(
                        alreadyAppliedSearch = state.search,
                    )
                )
            }

            State.NoSearch -> {
                selectedMediaTypes.value = searchDefaults.mediaTypes
                userQuery.value = searchDefaults.userQuery
                includePrivateContent.value = searchDefaults.includePrivate
                selectedAlbumUid.value = searchDefaults.albumUid

                stateSubject.onNext(
                    State.ConfiguringSearch(
                        alreadyAppliedSearch = null
                    )
                )
            }

            is State.ConfiguringSearch -> {
                // Nothing to change.
            }
        }

        updateExternalData()
    }

    private fun updateExternalData() {
        bookmarksRepository.updateIfNotFresh()
        albumsViewModel.updateIfNotFresh()
    }

    fun onConfigurationViewClosing() {
        log.debug {
            "onConfigurationViewClosing(): configuration_view_is_closing"
        }

        when (val state = stateSubject.value!!) {
            is State.AppliedSearch,
            is State.NoSearch -> {
                // Expected.
            }
            is State.ConfiguringSearch -> {
                if (state.alreadyAppliedSearch != null) {
                    stateSubject.onNext(State.AppliedSearch(state.alreadyAppliedSearch))
                } else {
                    stateSubject.onNext(State.NoSearch)
                }
            }
        }
    }

    fun onSearchClicked() {
        log.debug {
            "onSearchClicked(): search_clicked:" +
                    "\ncanApplyConfiguredSearch=$canApplyConfiguredSearch"
        }

        if (canApplyConfiguredSearch) {
            applyConfiguredSearch()
        }
    }

    private fun applyConfiguredSearch() {
        check(stateSubject.value is State.ConfiguringSearch) {
            "The search can only be applied while configuring"
        }

        val config = SearchConfig(
            mediaTypes = selectedMediaTypes.value ?: emptySet(),
            userQuery = userQuery.value!!.trim(),
            albumUid = selectedAlbumUid.value,
            before = null,
            includePrivate = includePrivateContent.value == true,
        )
        val bookmark = bookmarksRepository.findByConfig(config)
        val appliedSearch =
            if (bookmark != null)
                AppliedGallerySearch.Bookmarked(bookmark)
            else
                AppliedGallerySearch.Custom(config)

        log.debug {
            "applySearch(): applying_search:" +
                    "\nsearch=$appliedSearch"
        }

        stateSubject.onNext(State.AppliedSearch(appliedSearch))
    }

    fun onResetClicked() {
        log.debug {
            "onResetClicked(): reset_clicked"
        }

        stateSubject.onNext(State.NoSearch)
    }

    fun onAddBookmarkClicked() {
        val appliedSearchState = (stateSubject.value as? State.AppliedSearch).checkNotNull {
            "Add bookmark button is only clickable in the applied search state"
        }
        check(appliedSearchState.search !is AppliedGallerySearch.Bookmarked) {
            "Add bookmark button can't be clicked in the applied bookmarked search state"
        }

        log.debug {
            "onAddBookmarkClicked(): add_bookmark_clicked"
        }

        eventsSubject.onNext(
            Event.OpenBookmarkDialog(
                searchConfig = appliedSearchState.search.config,
                existingBookmark = null,
            )
        )
    }

    fun onEditBookmarkClicked() {
        val appliedSearchState = (stateSubject.value as? State.AppliedSearch).checkNotNull {
            "Edit bookmark button is only clickable in the applied search state"
        }
        val bookmark = (appliedSearchState.search as? AppliedGallerySearch.Bookmarked)
            ?.bookmark
            .checkNotNull {
                "Edit bookmark button is only clickable when a bookmarked search is applied"
            }

        log.debug {
            "onEditBookmarkClicked(): edit_bookmark_clicked:" +
                    "\nbookmark=$bookmark"
        }

        eventsSubject.onNext(
            Event.OpenBookmarkDialog(
                searchConfig = bookmark.searchConfig,
                existingBookmark = bookmark,
            )
        )
    }

    fun onBookmarkChipClicked(item: SearchBookmarkItem) {
        check(stateSubject.value is State.ConfiguringSearch) {
            "Bookmark chips are clickable only in the search configuration state"
        }

        log.debug {
            "onBookmarkChipClicked(): chip_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            configureAndApplySearchFromBookmark(item.source)
        }
    }

    private fun configureAndApplySearchFromBookmark(bookmark: SearchBookmark) {
        selectedMediaTypes.value = bookmark.searchConfig.mediaTypes
            // A bookmark may have more media types than allowed now
            // (due to the intent filter).
            .intersect(availableMediaTypes.value!!)
        userQuery.value = bookmark.searchConfig.userQuery
        includePrivateContent.value = bookmark.searchConfig.includePrivate
        selectedAlbumUid.value = bookmark.searchConfig.albumUid

        log.debug {
            "applySearchFromBookmark(): configured_search_from_bookmark:" +
                    "\nbookmark=$bookmark"
        }

        applyConfiguredSearch()
    }

    fun onBookmarkChipEditClicked(item: SearchBookmarkItem) {
        check(stateSubject.value is State.ConfiguringSearch) {
            "Bookmark chip edit buttons are clickable only in the search configuration state"
        }

        log.debug {
            "onBookmarkChipEditClicked(): chip_edit_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            eventsSubject.onNext(
                Event.OpenBookmarkDialog(
                    searchConfig = item.source.searchConfig,
                    existingBookmark = item.source,
                )
            )
        }
    }

    /**
     * @param placedAfter Item after which the chip is placed, or null if placed at the start
     */
    fun onBookmarkChipMoved(item: SearchBookmarkItem, placedAfter: SearchBookmarkItem?) {
        check(stateSubject.value is State.ConfiguringSearch) {
            "Bookmark chips are movable only in the search configuration state"
        }

        log.debug {
            "onBookmarkChipMoved(): chip_moved:" +
                    "\nitem=$item" +
                    "\nplacedAfter=$placedAfter"
        }

        if (item.source != null && (placedAfter == null || placedAfter.source != null)) {
            bookmarksRepository.placeAfter(
                id = placedAfter?.source?.id,
                bookmark = item.source,
            )
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { areBookmarksCurrentlyMoving.value = true }
                .doOnTerminate { areBookmarksCurrentlyMoving.value = false }
                .doOnComplete {
                    log.debug {
                        "onBookmarkChipMoved(): movement_applied"
                    }
                }
                .subscribe()
                .addToCloseables(this)
        }
    }

    fun onBookmarkChipsSwapped(
        first: SearchBookmarkItem,
        second: SearchBookmarkItem,
    ) {
        check(stateSubject.value is State.ConfiguringSearch) {
            "Bookmark chips are movable only in the search configuration state"
        }

        log.debug {
            "onBookmarkChipsSwapped(): chips_swapped:" +
                    "\nfirst=$first" +
                    "\nsecond=$second"
        }

        if (first.source != null && second.source != null) {
            bookmarksRepository.swapPositions(
                first = first.source,
                second = second.source,
            )
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { areBookmarksCurrentlyMoving.value = true }
                .doOnTerminate { areBookmarksCurrentlyMoving.value = false }
                .doOnComplete {
                    log.debug {
                        "onBookmarkChipsSwapped(): movement_applied"
                    }
                }
                .subscribe()
                .addToCloseables(this)
        }
    }

    fun onSearchFiltersGuideClicked() {
        log.debug {
            "onSearchFiltersGuideClicked(): opening_guide:" +
                    "\nurl=$searchFiltersGuideUrl"
        }

        eventsSubject.onNext(Event.OpenUrl(url = searchFiltersGuideUrl))
    }

    sealed interface State {
        object NoSearch : State
        data class ConfiguringSearch(val alreadyAppliedSearch: AppliedGallerySearch?) : State
        data class AppliedSearch(val search: AppliedGallerySearch) : State
    }

    sealed interface Event {
        class OpenBookmarkDialog(
            val searchConfig: SearchConfig,
            val existingBookmark: SearchBookmark?,
        ) : Event

        class OpenUrl(val url: String) : Event
    }
}