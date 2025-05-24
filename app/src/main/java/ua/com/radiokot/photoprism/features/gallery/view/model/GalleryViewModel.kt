package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.WebPageInteractionRequiredException
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.envconnection.logic.DisconnectFromEnvUseCase
import ua.com.radiokot.photoprism.features.ext.memories.view.model.GalleryMemoriesListViewModel
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMonth
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.util.BackPressActionsStack

class GalleryViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val disconnectFromEnvUseCase: DisconnectFromEnvUseCase,
    private val connectionParams: EnvConnectionParams,
    val searchViewModel: GallerySearchViewModel,
    val fastScrollViewModel: GalleryFastScrollViewModel,
    val memoriesListViewModel: GalleryMemoriesListViewModel,
    private val listViewModel: GalleryListViewModelImpl,
    private val galleryMediaDownloadActionsViewModel: GalleryMediaDownloadActionsViewModelDelegate,
    private val galleryMediaRemoteActionsViewModel: GalleryMediaRemoteActionsViewModelDelegate,
) : ViewModel(),
    GalleryListViewModel by listViewModel,
    GalleryMediaDownloadActionsViewModel by galleryMediaDownloadActionsViewModel,
    GalleryMediaRemoteActionsViewModel by galleryMediaRemoteActionsViewModel {

    private val log = kLogger("GalleryVM")
    private val mediaRepositoryChanges = BehaviorSubject.create<MediaRepositoryChange>()

    // Current search config regardless the fast scroll.
    private var currentSearchConfig: SearchConfig = SearchConfig.DEFAULT
    private var isInitialized = false
    private val currentMediaRepository: SimpleGalleryMediaRepository?
        get() = mediaRepositoryChanges.value?.repository
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOnMain()
    private val stateSubject = BehaviorSubject.create<State>()
    val state: Observable<State> = stateSubject.observeOnMain()
    val currentState: State
        get() = stateSubject.value!!
    val mainError = MutableLiveData<Error?>(null)
    var canLoadMore = true
        private set

    private val backPressActionsStack = BackPressActionsStack()
    val backPressedCallback: OnBackPressedCallback =
        backPressActionsStack.onBackPressedCallback
    private val resetFastScrollBackPressAction = {
        fastScrollViewModel.reset(isInitiatedByUser = true)
    }
    private val resetSearchOnBackPress = {
        searchViewModel.resetSearch()
    }
    private val closeSearchConfigurationOnBackPress = {
        searchViewModel.switchBackFromConfiguring()
    }
    private val switchBackToViewingOnBackPress = {
        switchToViewing()
    }

    fun initSelectionForAppOnce(
        requestedMimeType: String?,
        allowMultiple: Boolean,
    ) {
        if (isInitialized) {
            log.debug {
                "initSelectionForAppOnce(): already_initialized"
            }

            return
        }

        val allowedMediaTypes: Set<GalleryMedia.TypeName>? = when {
            requestedMimeType == null ->
                null

            requestedMimeType.startsWith("image/") ->
                setOf(
                    GalleryMedia.TypeName.IMAGE,
                    GalleryMedia.TypeName.RAW,
                    GalleryMedia.TypeName.ANIMATED,
                    GalleryMedia.TypeName.VECTOR,
                )

            requestedMimeType.startsWith("video/") ->
                setOf(
                    GalleryMedia.TypeName.VIDEO,
                    GalleryMedia.TypeName.LIVE,
                )

            else ->
                null
        }

        if (allowedMediaTypes != null) {
            searchViewModel.availableMediaTypes.value = allowedMediaTypes
        }

        stateSubject.onNext(
            State.Selecting.ForOtherApp(
                allowedMediaTypes = allowedMediaTypes,
                allowMultiple = allowMultiple,
            )
        )

        if (!allowMultiple) {
            listViewModel.initSelectingSingle(
                onSingleMediaSelected = { media ->
                    galleryMediaDownloadActionsViewModel.downloadAndReturnGalleryMedia(
                        media = listOf(media),
                    )
                },
                shouldPostItemsNow = { repositoryToPostFrom ->
                    repositoryToPostFrom == currentMediaRepository
                },
            )
        } else {
            listViewModel.initSelectingMultiple(
                shouldPostItemsNow = { repositoryToPostFrom ->
                    repositoryToPostFrom == currentMediaRepository
                },
            )
        }

        initCommon()

        log.debug {
            "initSelectionForAppOnce(): initialized_selection:" +
                    "\nrequestedMimeType=$requestedMimeType," +
                    "\nallowMultiple=$allowMultiple," +
                    "\nallowedMediaTypes=$allowedMediaTypes"
        }

        isInitialized = true
    }

    fun initViewingOnce(
        requestedBookmarkId: Long?,
    ) {
        if (isInitialized) {
            log.debug {
                "initViewingOnce(): already_initialized"
            }

            return
        }

        if (requestedBookmarkId != null) {
            searchViewModel.applyBookmarkByIdAsync(
                bookmarkId = requestedBookmarkId,
            )
        }

        stateSubject.onNext(State.Viewing)

        listViewModel.initViewing(
            onSwitchedFromViewingToSelecting = {
                stateSubject.onNext(State.Selecting.ForUser)
                backPressActionsStack.pushUniqueAction(switchBackToViewingOnBackPress)
            },
            onSwitchedFromSelectingToViewing = {
                stateSubject.onNext(State.Viewing)
                backPressActionsStack.removeAction(switchBackToViewingOnBackPress)
            },
            shouldPostItemsNow = { repositoryToPostFrom ->
                repositoryToPostFrom == currentMediaRepository
            },
        )

        initCommon()

        log.debug {
            "initViewingOnce(): initialized_viewing:" +
                    "\nrequestedBookmarkId=$requestedBookmarkId"
        }

        isInitialized = true
    }

    private fun initCommon() {
        subscribeToSearch()
        subscribeToFastScroll()
        subscribeToRepositoryChanges()

        resetRepositoryAndSearchConfig()
    }

    private fun resetRepositoryAndSearchConfig() {
        currentSearchConfig = getStateDefaultSearchConfig()

        mediaRepositoryChanges.onNext(
            MediaRepositoryChange.ResetToInitial(
                galleryMediaRepositoryFactory.get(currentSearchConfig),
            )
        )
    }

    private fun getStateDefaultSearchConfig(): SearchConfig =
        when (val currentState = this.currentState) {
            is State.Selecting.ForOtherApp ->
                SearchConfig.DEFAULT
                    .withOnlyAllowedMediaTypes(currentState.allowedMediaTypes)

            else ->
                SearchConfig.DEFAULT
        }

    private fun subscribeToSearch() {
        searchViewModel.state.subscribe { state ->
            log.debug {
                "subscribeToSearch(): received_new_state:" +
                        "\nstate=$state"
            }

            when (state) {
                is GallerySearchViewModel.State.Applied -> {
                    val currentState = this.currentState
                    val searchConfigToApply: SearchConfig =
                        if (currentState is State.Selecting.ForOtherApp) {
                            // If we are selecting the content,
                            // make sure we do not apply search that goes beyond the allowed media types.
                            state.search.config.withOnlyAllowedMediaTypes(
                                allowedMediaTypes = currentState.allowedMediaTypes,
                            )
                        } else {
                            state.search.config
                        }
                    currentSearchConfig = searchConfigToApply

                    val searchMediaRepository = galleryMediaRepositoryFactory
                        .get(searchConfigToApply)

                    fastScrollViewModel.reset(isInitiatedByUser = false)

                    if (searchMediaRepository != mediaRepositoryChanges.value?.repository) {
                        mediaRepositoryChanges.onNext(
                            MediaRepositoryChange.Search(
                                searchMediaRepository
                            )
                        )
                    }

                    // Make the back button press reset the search.
                    backPressActionsStack.removeAction(closeSearchConfigurationOnBackPress)
                    backPressActionsStack.pushUniqueAction(resetSearchOnBackPress)
                }

                GallerySearchViewModel.State.NoSearch -> {
                    fastScrollViewModel.reset(isInitiatedByUser = false)
                    resetRepositoryAndSearchConfig()

                    // When search is switched to NoSearch, no need to reset
                    // fast scroll or close the configuration view.
                    backPressActionsStack.removeAction(resetSearchOnBackPress)
                    backPressActionsStack.removeAction(resetFastScrollBackPressAction)
                    backPressActionsStack.removeAction(closeSearchConfigurationOnBackPress)
                }

                is GallerySearchViewModel.State.Configuring -> {
                    // Make the back button press close the search configuration view.
                    backPressActionsStack.pushUniqueAction(closeSearchConfigurationOnBackPress)
                }
            }

            log.debug {
                "subscribeToSearch(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.autoDispose(this)
    }

    private fun subscribeToFastScroll() {
        fastScrollViewModel.events.subscribeBy { event ->
            log.debug {
                "subscribeToFastScroll(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is GalleryFastScrollViewModel.Event.DraggingAtMonth -> {
                    handleFastScroll(
                        month = event.month,
                        isScrolledToTheTop = event.isTopMonth,
                    )
                }

                is GalleryFastScrollViewModel.Event.Reset -> {
                    if (event.isInitiatedByUser) {
                        handleFastScroll(
                            month = null,
                            isScrolledToTheTop = true,
                        )
                    }
                }
            }

            log.debug {
                "subscribeToFastScroll(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.autoDispose(this)
    }

    /**
     * @param month current month the fast scroll is staying, if any
     * @param isScrolledToTheTop whether the fast scroll is at the top
     */
    private fun handleFastScroll(
        month: GalleryMonth?,
        isScrolledToTheTop: Boolean,
    ) {
        val searchConfigForMonth: SearchConfig =
            if (isScrolledToTheTop || month == null)
            // For top month we don't need to alter the "before" date,
            // if altered the result is the same as if the date is not set at all.
                currentSearchConfig
            else
                currentSearchConfig.copy(beforeLocal = month.nextDayAfter)

        // Always reset the scroll.
        // If the list is scrolled manually, setting fast scroll to the same month
        // must bring it back to the top.
        eventsSubject.onNext(Event.ResetScroll)

        // Make the back button press reset fast scroll if it is not on the top.
        backPressActionsStack.removeAction(resetFastScrollBackPressAction)
        if (!isScrolledToTheTop) {
            backPressActionsStack.pushUniqueAction(resetFastScrollBackPressAction)
        }

        // We need to change the repo if scrolled to the top month
        // (return to initial state before scrolling)
        // or if the search config for the month differs from the current.
        if (isScrolledToTheTop || searchConfigForMonth != currentSearchConfig) {
            log.debug {
                "subscribeToFastScroll(): switching_to_month_search_config:" +
                        "\nconfig=$searchConfigForMonth"
            }

            if (searchConfigForMonth != SearchConfig.DEFAULT) {
                val repositoryForMonth =
                    galleryMediaRepositoryFactory.get(searchConfigForMonth)

                mediaRepositoryChanges.onNext(
                    MediaRepositoryChange.FastScroll(repositoryForMonth)
                )
            } else {
                resetRepositoryAndSearchConfig()
            }
        }
    }

    private fun subscribeToRepositoryChanges() {
        mediaRepositoryChanges
            .distinctUntilChanged()
            .subscribe { change ->
                subscribeToRepository()
                update()

                eventsSubject.onNext(Event.ResetScroll)

                if (change !is MediaRepositoryChange.FastScroll) {
                    fastScrollViewModel.setMediaRepository(change.repository)
                }

                memoriesListViewModel.isViewRequired =
                    change is MediaRepositoryChange.ResetToInitial
                            && currentState !is State.Selecting.ForOtherApp
            }
            .autoDispose(this)
    }

    private var repositorySubscriptionDisposable: CompositeDisposable? = null
    private fun subscribeToRepository() {
        repositorySubscriptionDisposable?.dispose()

        val disposable = CompositeDisposable()
        repositorySubscriptionDisposable = disposable

        val currentMediaRepository = this.currentMediaRepository
            ?: return

        log.debug {
            "subscribeToRepository(): subscribing:" +
                    "\nrepository=$currentMediaRepository"
        }

        currentMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { items ->
                mainError.value = when {
                    items.isEmpty() && currentSearchConfig.mediaTypes?.isEmpty() == true ->
                        Error.SearchDoesNotFitAllowedTypes

                    items.isEmpty() && !currentMediaRepository.isNeverUpdated ->
                        Error.NoMediaFound

                    else ->
                        null
                }

                postGalleryItemsAsync(currentMediaRepository)
            }
            .addTo(disposable)

        currentMediaRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { isLoading ->
                canLoadMore = !currentMediaRepository.noMoreItems
                this.isLoading.value = isLoading

                // Dismiss the main error when something is loading.
                if (isLoading) {
                    mainError.value = null
                }
            }
            .addTo(disposable)

        currentMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) { "subscribeToRepository(): error_occurred" }

                val contentLoadingError = Error.ContentLoadingError(
                    GalleryContentLoadingError.from(error)
                )

                if (itemList.value.isNullOrEmpty()) {
                    mainError.value = contentLoadingError
                } else {
                    eventsSubject.onNext(Event.ShowFloatingError(contentLoadingError))
                }

                if (error is WebPageInteractionRequiredException) {
                    eventsSubject.onNext(
                        Event.OpenWebViewerForRedirectHandling(
                            url = connectionParams.apiUrl.toString(),
                        )
                    )
                }
            }
            .addTo(disposable)

        disposable.autoDispose(this)
    }

    private fun update(force: Boolean = false) {
        val currentMediaRepository = this.currentMediaRepository
            ?: return

        if (!force) {
            currentMediaRepository.updateIfNotFresh()
        } else {
            currentMediaRepository.update()
        }

        fastScrollViewModel.updateBubbles()
    }

    fun loadMore() {
        val currentMediaRepository = this.currentMediaRepository
            ?: return

        if (!currentMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }

            currentMediaRepository.loadMore()
        }
    }

    fun onMainErrorRetryClicked() {
        update()
    }

    fun onFloatingErrorRetryClicked() {
        loadMore()
    }

    fun onLoadingFooterLoadMoreClicked() {
        loadMore()
    }

    fun onPreferencesClicked() {
        eventsSubject.onNext(Event.OpenPreferences)
    }

    fun onFoldersClicked() {
        eventsSubject.onNext(
            Event.OpenAlbums(
                albumType = Album.TypeName.FOLDER,
                defaultSearchConfig = getStateDefaultSearchConfig(),
            )
        )
    }

    fun onAlbumsClicked() {
        eventsSubject.onNext(
            Event.OpenAlbums(
                albumType = Album.TypeName.ALBUM,
                defaultSearchConfig = getStateDefaultSearchConfig(),
            )
        )
    }

    fun onCalendarClicked() {
        eventsSubject.onNext(
            Event.OpenAlbums(
                albumType = Album.TypeName.MONTH,
                defaultSearchConfig = getStateDefaultSearchConfig(),
            )
        )
    }

    fun onFavoritesClicked() {
        eventsSubject.onNext(
            Event.OpenFavorites(
                repositoryParams = SimpleGalleryMediaRepository.Params(
                    searchConfig = getStateDefaultSearchConfig().copy(
                        onlyFavorite = true,
                    ),
                ),
            )
        )
    }

    fun onDoneMultipleSelectionClicked() {
        val currentState = this.currentState
        check(currentState is State.Selecting.ForOtherApp && currentState.allowMultiple) {
            "Done multiple selection button is only clickable when selecting multiple for other app"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Done multiple selection button is only clickable when something is selected"
        }

        galleryMediaDownloadActionsViewModel.downloadAndReturnGalleryMedia(
            media = selectedMediaByUid.values,
        )
    }

    fun onShareMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Share multiple selection button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Share multiple selection button is only clickable when something is selected"
        }

        galleryMediaDownloadActionsViewModel.downloadAndShareGalleryMedia(
            media = selectedMediaByUid.values,
            onShared = {
                switchToViewing()
            }
        )
    }

    fun onDownloadMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Download multiple selection button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Download multiple selection button is only clickable when something is selected"
        }

        galleryMediaDownloadActionsViewModel.downloadGalleryMediaToExternalStorage(
            media = selectedMediaByUid.values,
            onDownloadFinished = {
                switchToViewing()
            }
        )
    }

    fun onAddToAlbumMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Adding multiple selection to album button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Adding multiple selection to album button is only clickable when something is selected"
        }

        galleryMediaRemoteActionsViewModel.addGalleryMediaToAlbum(
            mediaUids = selectedMediaByUid.keys.toList(),
            onStarted = ::switchToViewing,
        )
    }

    fun onArchiveMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Archive multiple selection button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Archive multiple selection button is only clickable when something is selected"
        }

        galleryMediaRemoteActionsViewModel.archiveGalleryMedia(
            mediaUids = selectedMediaByUid.keys.toList(),
            currentMediaRepository = currentMediaRepository.checkNotNull {
                "There must be a media repository to archive items from"
            },
            onStarted = ::switchToViewing,
        )
    }

    fun onDeleteMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Delete multiple selection button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Delete multiple selection button is only clickable when something is selected"
        }

        galleryMediaRemoteActionsViewModel.deleteGalleryMedia(
            mediaUids = selectedMediaByUid.keys.toList(),
            currentMediaRepository = currentMediaRepository.checkNotNull {
                "There must be a media repository to delete items from"
            },
            onStarted = ::switchToViewing,
        )
    }

    fun onScreenResumedAfterMovedBackWithBackButton() {
        log.debug {
            "onScreenResumedAfterMovedBackWithBackButton(): invalidate_all_cached_repos"
        }

        // When resuming from the background, invalidate all the cached repos
        // to load the fresh data.
        galleryMediaRepositoryFactory.invalidateAllCached()

        update(force = true)
        searchViewModel.updateExternalData(force = true)

        // Reset the scroll and, more importantly, pagination
        // as the number of list items may decrease.
        eventsSubject.onNext(Event.ResetScroll)
    }

    fun onErrorDisconnectClicked() {
        log.debug { "onErrorDisconnectClicked(): begin_disconnect" }

        disconnectFromEnvUseCase()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    eventsSubject.onNext(
                        Event.GoToEnvConnection(
                            rootUrl = connectionParams.rootUrl.toString(),
                        )
                    )
                },
                onError = { error ->
                    log.error(error) {
                        "disconnect(): error_occurred"
                    }

                    eventsSubject.onNext(
                        Event.ShowFloatingError(
                            Error.ContentLoadingError(
                                GalleryContentLoadingError.GeneralFailure(
                                    shortSummary = error.shortSummary
                                )
                            )
                        )
                    )
                }
            )
            .autoDispose(this)
    }

    private fun switchToViewing() {
        assert(currentState is State.Selecting.ForUser) {
            "Switching to viewing is only possible while selecting to share"
        }

        listViewModel.switchFromSelectingToViewing()
    }

    fun onDownloadedFilesShared() {
        switchToViewing()
    }

    fun onSwipeRefreshPulled() {
        log.debug {
            "onSwipeRefreshPulled(): force_updating"
        }

        update(force = true)
    }

    fun onWebViewerHandledRedirect() {
        log.debug {
            "onWebViewerHandledRedirect(): force_updating"
        }

        update(force = true)
    }

    sealed interface State {
        /**
         * Viewing the gallery content.
         */
        object Viewing : State

        /**
         * Viewing the gallery content to select something.
         */
        sealed class Selecting(
            /**
             * Whether selection of multiple items is allowed or not.
             */
            val allowMultiple: Boolean,
        ) : State {

            /**
             * Selecting to return the files to the requesting app.
             */
            class ForOtherApp(
                /**
                 * Non-empty optional set of allowed media types (e.g. only images)
                 */
                val allowedMediaTypes: Set<GalleryMedia.TypeName>?,
                allowMultiple: Boolean,
            ) : Selecting(
                allowMultiple = allowMultiple,
            ) {
                init {
                    require(allowedMediaTypes == null || allowedMediaTypes.isNotEmpty()) {
                        "The set of allowed types must either be null or not empty"
                    }
                }
            }

            /**
             * Selecting to share the files with any app of the user's choice.
             */
            object ForUser : Selecting(
                allowMultiple = true,
            )
        }
    }

    sealed interface Event {
        /**
         * Reset the scroll (to the top) and the infinite scrolling.
         */
        object ResetScroll : Event

        /**
         * Show a dismissible floating error.
         *
         * The [onFloatingErrorRetryClicked] method should be called
         * if the error assumes retrying.
         */
        @JvmInline
        value class ShowFloatingError(val error: Error) : Event

        object OpenPreferences : Event

        class OpenAlbums(
            val albumType: Album.TypeName,
            val defaultSearchConfig: SearchConfig,
        ) : Event

        class OpenFavorites(
            val repositoryParams: SimpleGalleryMediaRepository.Params,
        ) : Event

        /**
         * Close the screen and go to the connection,
         * providing the [rootUrl] parameter.
         */
        class GoToEnvConnection(val rootUrl: String) : Event

        /**
         * Call [onWebViewerHandledRedirect] on successful result.
         */
        class OpenWebViewerForRedirectHandling(val url: String) : Event
    }

    sealed interface Error {
        @JvmInline
        value class ContentLoadingError(
            val contentLoadingError: GalleryContentLoadingError,
        ) : Error

        /**
         * Nothing is found for the given search.
         * The gallery may be empty as well.
         */
        object NoMediaFound : Error

        /**
         * Nothing is found because all the types from the given search
         * are not allowed by the requesting app.
         */
        object SearchDoesNotFitAllowedTypes : Error
    }

    private sealed class MediaRepositoryChange(
        val repository: SimpleGalleryMediaRepository,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MediaRepositoryChange) return false

            if (repository != other.repository) return false

            return true
        }

        override fun hashCode(): Int {
            return repository.hashCode()
        }

        /**
         * Change caused by chronological fast scroll.
         */
        class FastScroll(repository: SimpleGalleryMediaRepository) :
            MediaRepositoryChange(repository)

        /**
         * Change caused by applying search.
         */
        class Search(repository: SimpleGalleryMediaRepository) :
            MediaRepositoryChange(repository)

        /**
         * Change caused by resetting it to the initial one.
         */
        class ResetToInitial(repository: SimpleGalleryMediaRepository) :
            MediaRepositoryChange(repository)
    }
}
