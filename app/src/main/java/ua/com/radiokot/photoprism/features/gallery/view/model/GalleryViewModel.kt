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
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.capitalized
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.envconnection.logic.DisconnectFromEnvUseCase
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMonth
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.util.BackPressActionsStack
import ua.com.radiokot.photoprism.util.LocalDate
import java.io.File
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.DateFormat
import kotlin.collections.set

class GalleryViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val dateHeaderUtcDayYearDateFormat: DateFormat,
    private val dateHeaderUtcDayDateFormat: DateFormat,
    private val dateHeaderUtcMonthYearDateFormat: DateFormat,
    private val dateHeaderUtcMonthDateFormat: DateFormat,
    private val internalDownloadsDir: File,
    private val externalDownloadsDir: File,
    private val disconnectFromEnvUseCase: DisconnectFromEnvUseCase,
    val downloadMediaFileViewModel: DownloadMediaFileViewModel,
    val searchViewModel: GallerySearchViewModel,
    val fastScrollViewModel: GalleryFastScrollViewModel,
) : ViewModel() {
    private val log = kLogger("GalleryVM")
    private val mediaRepositoryChanges = BehaviorSubject.create<MediaRepositoryChange>()
    private val currentLocalDate = LocalDate()

    // Current search config regardless the fast scroll.
    private var currentSearchConfig: SearchConfig? = null
    private var isInitialized = false
    private val currentMediaRepository: SimpleGalleryMediaRepository?
        get() = mediaRepositoryChanges.value?.repository
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<GalleryListItem>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.toMainThreadObservable()
    private val stateSubject = BehaviorSubject.create<State>()
    val state: Observable<State> = stateSubject.toMainThreadObservable()
    val mainError = MutableLiveData<Error?>(null)
    var canLoadMore = true
        private set
    private val multipleSelectionFilesByMediaUid = linkedMapOf<String, GalleryMedia.File>()
    val multipleSelectionItemsCount: MutableLiveData<Int> = MutableLiveData(0)

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

        log.debug {
            "initSelectionForAppOnce(): initialized_selection:" +
                    "\nrequestedMimeType=$requestedMimeType," +
                    "\nallowMultiple=$allowMultiple," +
                    "\nallowedMediaTypes=$allowedMediaTypes"
        }

        stateSubject.onNext(
            State.Selecting.ForOtherApp(
                allowedMediaTypes = allowedMediaTypes,
                allowMultiple = allowMultiple,
            )
        )

        initCommon()

        isInitialized = true
    }

    fun initViewingOnce() {
        if (isInitialized) {
            log.debug {
                "initViewingOnce(): already_initialized"
            }

            return
        }

        log.debug {
            "initViewingOnce(): initialized_viewing"
        }

        stateSubject.onNext(State.Viewing)

        initCommon()

        isInitialized = true
    }

    private fun initCommon() {
        subscribeToSearch()
        subscribeToFastScroll()
        subscribeToRepositoryChanges()
        resetRepositoryToInitial()
    }

    private fun resetRepositoryToInitial() {
        val currentState = stateSubject.value

        if (currentState is State.Selecting.ForOtherApp) {
            val searchConfig =
                SearchConfig.DEFAULT
                    .withOnlyAllowedMediaTypes(currentState.allowedMediaTypes)

            currentSearchConfig = searchConfig
            mediaRepositoryChanges.onNext(
                MediaRepositoryChange.Other(
                    galleryMediaRepositoryFactory.getForSearch(searchConfig),
                )
            )
        } else {
            currentSearchConfig = null
            mediaRepositoryChanges.onNext(
                MediaRepositoryChange.Other(
                    galleryMediaRepositoryFactory.get(),
                )
            )
        }
    }

    private fun subscribeToSearch() {
        searchViewModel.state.subscribe { state ->
            log.debug {
                "subscribeToSearch(): received_new_state:" +
                        "\nstate=$state"
            }

            when (state) {
                is GallerySearchViewModel.State.Applied -> {
                    val currentState = stateSubject.value
                    val searchConfigToApply: SearchConfig =
                        if (currentState is State.Selecting.ForOtherApp) {
                            // If we are selecting the content,
                            // make sure we do not apply search that overcomes the allowed media types.
                            state.search.config.withOnlyAllowedMediaTypes(
                                allowedMediaTypes = currentState.allowedMediaTypes,
                            )
                        } else {
                            state.search.config
                        }
                    currentSearchConfig = searchConfigToApply

                    val searchMediaRepository = galleryMediaRepositoryFactory
                        .getForSearch(searchConfigToApply)

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
                    resetRepositoryToInitial()

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
        val searchConfigForMonth: SearchConfig? =
            if (isScrolledToTheTop || month == null)
            // For top month we don't need to alter the "before" date,
            // if altered the result is the same as if the date is not set at all.
                currentSearchConfig
            else
                (currentSearchConfig ?: SearchConfig.DEFAULT)
                    .copy(beforeLocal = month.nextDayAfter)

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

            if (searchConfigForMonth != null) {
                val repositoryForMonth =
                    galleryMediaRepositoryFactory.getForSearch(searchConfigForMonth)

                mediaRepositoryChanges.onNext(
                    MediaRepositoryChange.FastScroll(
                        repositoryForMonth
                    )
                )
            } else {
                resetRepositoryToInitial()
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

        // Do not observe items on the main thread,
        // item preparation should not block the UI.
        currentMediaRepository.items
            .observeOn(Schedulers.computation())
            .subscribe { postGalleryItems() }
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

                val viewError = when (error) {
                    is UnknownHostException,
                    is NoRouteToHostException,
                    is SocketTimeoutException ->
                        Error.LibraryNotAccessible

                    is InvalidCredentialsException ->
                        Error.CredentialsHaveBeenChanged

                    else ->
                        Error.LoadingFailed(error.shortSummary)
                }

                if (itemsList.value.isNullOrEmpty()) {
                    mainError.value = viewError
                } else {
                    eventsSubject.onNext(Event.ShowFloatingError(viewError))
                }
            }
            .addTo(disposable)

        disposable.autoDispose(this)
    }

    private fun postGalleryItems() {
        val repository = currentMediaRepository.checkNotNull {
            "There must be a media repository to post items from"
        }
        val galleryMediaList = repository.itemsList

        mainError.postValue(
            when {
                galleryMediaList.isEmpty() && currentSearchConfig?.mediaTypes?.isEmpty() == true ->
                    Error.SearchDoesNotFitAllowedTypes

                galleryMediaList.isEmpty() && !repository.isNeverUpdated ->
                    Error.NoMediaFound

                else ->
                    // Dismiss the main error when there are items.
                    null
            }
        )

        val newListItems = mutableListOf<GalleryListItem>()
        val currentState = stateSubject.value
        val areViewButtonsVisible = currentState is State.Selecting
        val areSelectionViewsVisible = currentState is State.Selecting && currentState.allowMultiple

        // Add date headers.
        galleryMediaList
            .forEachIndexed { i, galleryMedia ->
                val takenAtLocal = galleryMedia.takenAtLocal

                if (i == 0 && !takenAtLocal.isSameMonthAs(currentLocalDate)
                    || i != 0 && !takenAtLocal.isSameMonthAs(galleryMediaList[i - 1].takenAtLocal)
                ) {
                    val formattedMonth =
                        if (takenAtLocal.isSameYearAs(currentLocalDate))
                            dateHeaderUtcMonthDateFormat.format(takenAtLocal)
                        else
                            dateHeaderUtcMonthYearDateFormat.format(takenAtLocal)

                    newListItems.add(
                        GalleryListItem.Header.month(
                            text = formattedMonth.capitalized(),
                        )
                    )
                }

                if (i == 0 || !takenAtLocal.isSameDayAs(galleryMediaList[i - 1].takenAtLocal)) {
                    newListItems.add(
                        if (takenAtLocal.isSameDayAs(currentLocalDate))
                            GalleryListItem.Header.day(
                                textRes = R.string.today,
                            )
                        else {
                            val formattedDate =
                                if (takenAtLocal.isSameYearAs(currentLocalDate))
                                    dateHeaderUtcDayDateFormat.format(takenAtLocal)
                                else
                                    dateHeaderUtcDayYearDateFormat.format(takenAtLocal)

                            GalleryListItem.Header.day(
                                text = formattedDate.capitalized(),
                            )
                        }
                    )
                }

                newListItems.add(
                    GalleryListItem.Media(
                        source = galleryMedia,
                        isViewButtonVisible = areViewButtonsVisible,
                        isSelectionViewVisible = areSelectionViewsVisible,
                        isMediaSelected = multipleSelectionFilesByMediaUid.containsKey(galleryMedia.uid),
                    )
                )
            }

        itemsList.postValue(newListItems)
    }

    private fun update(force: Boolean = false) {
        val currentMediaRepository = this.currentMediaRepository
            ?: return

        if (!force) {
            currentMediaRepository.updateIfNotFresh()
        } else {
            currentMediaRepository.update()
        }
    }

    fun loadMore() {
        val currentMediaRepository = this.currentMediaRepository
            ?: return

        if (!currentMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }

            currentMediaRepository.loadMore()
        }
    }

    fun onItemClicked(item: GalleryListItem) {
        log.debug {
            "onItemClicked(): gallery_item_clicked:" +
                    "\nitem=$item"
        }

        val media = (item as? GalleryListItem.Media)?.source
            ?: return

        when (val state = stateSubject.value.checkNotNull()) {
            is State.Selecting -> {
                if (state.allowMultiple) {
                    toggleMediaMultipleSelection(media)

                    // If the last selected item has been unselected,
                    // automatically switch back to viewing.
                    // But only if was selecting for user.
                    if (state is State.Selecting.ForUser
                        && multipleSelectionFilesByMediaUid.isEmpty()
                    ) {
                        log.debug { "onItemClicked(): unselected_last_switching_to_viewing" }

                        switchToViewing()
                    }
                } else {
                    selectMedia(media)
                }
            }

            is State.Viewing -> {
                openViewer(
                    media = media,
                    areActionsEnabled = true,
                )
            }
        }
    }

    fun onItemViewButtonClicked(item: GalleryListItem) {
        log.debug {
            "onItemViewButtonClicked(): gallery_item_view_button_clicked:" +
                    "\nitem=$item"
        }

        if (item !is GalleryListItem.Media) {
            return
        }

        if (item.source != null) {
            openViewer(
                media = item.source,
                areActionsEnabled = false,
            )
        }
    }

    fun onItemLongClicked(item: GalleryListItem) {
        log.debug {
            "onItemLongClicked(): gallery_item_long_clicked:" +
                    "\nitem=$item"
        }

        val media = (item as? GalleryListItem.Media)?.source
            ?: return

        when (stateSubject.value.checkNotNull()) {
            State.Viewing -> {
                log.debug { "onItemLongClicked(): switching_to_selecting_for_user" }

                switchToSelectingForUser(media)
            }

            else -> {
                // Long click does nothing in other states.
                log.debug { "onItemLongClicked(): ignored" }
            }
        }
    }

    /**
     * @param target an entry the user interacted with initiating the switch.
     */
    private fun switchToSelectingForUser(target: GalleryMedia) {
        assert(stateSubject.value is State.Viewing) {
            "Switching to selecting is only possible while viewing"
        }

        stateSubject.onNext(State.Selecting.ForUser)

        // Automatically select the target media.
        toggleMediaMultipleSelection(target)

        postGalleryItems()
        postMultipleSelectionItemsCount()

        // Make the back button press switch back to viewing.
        backPressActionsStack.pushUniqueAction(switchBackToViewingOnBackPress)
    }

    private fun selectMedia(media: GalleryMedia) {
        assert(stateSubject.value is State.Selecting) {
            "Media can only be selected handled in the corresponding state"
        }

        if (media.files.size > 1) {
            openFileSelectionDialog(media.files)
        } else {
            downloadAndReturnFile(media.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    private fun toggleMediaMultipleSelection(media: GalleryMedia) {
        assert(stateSubject.value is State.Selecting) {
            "Media multiple selection can only be toggled in the corresponding state"
        }

        if (multipleSelectionFilesByMediaUid.containsKey(media.uid)) {
            // When clicking currently selected media in the multiple selection state,
            // just unselect it.
            removeMediaFromMultipleSelection(media.uid)
        } else {
            if (media.files.size > 1) {
                openFileSelectionDialog(media.files)
            } else {
                addFileToMultipleSelection(media.files.firstOrNull().checkNotNull {
                    "There must be at least one file in the gallery media object"
                })
            }
        }
    }

    private fun addFileToMultipleSelection(file: GalleryMedia.File) {
        val currentState = stateSubject.value
        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Media file can only be added to the multiple selection in the corresponding state"
        }

        multipleSelectionFilesByMediaUid[file.mediaUid] = file

        log.debug {
            "addFileToMultipleSelection(): file_added:" +
                    "\nfile=$file," +
                    "\nmediaUid=${file.mediaUid}"
        }

        postGalleryItems()
        postMultipleSelectionItemsCount()
    }

    private fun removeMediaFromMultipleSelection(mediaUid: String) {
        val currentState = stateSubject.value
        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Media can only be removed from the multiple selection in the corresponding state"
        }

        multipleSelectionFilesByMediaUid.remove(mediaUid)

        log.debug {
            "removeMediaFromMultipleSelection(): media_removed:" +
                    "\nmediaUid=$mediaUid"
        }

        postGalleryItems()
        postMultipleSelectionItemsCount()
    }

    private fun postMultipleSelectionItemsCount() {
        multipleSelectionItemsCount.value = multipleSelectionFilesByMediaUid.keys.size
    }

    private fun openFileSelectionDialog(files: List<GalleryMedia.File>) {
        log.debug {
            "openFileSelectionDialog(): posting_open_event:" +
                    "\nfiles=$files"
        }

        eventsSubject.onNext(Event.OpenFileSelectionDialog(files))
    }

    fun onFileSelected(file: GalleryMedia.File) {
        val currentState = stateSubject.value
        check(currentState is State.Selecting) {
            "Media files can only be selected in the selection state"
        }

        log.debug {
            "onFileSelected(): file_selected:" +
                    "\nfile=$file"
        }

        if (currentState.allowMultiple) {
            addFileToMultipleSelection(file)
        } else {
            downloadAndReturnFile(file)
        }
    }

    private fun downloadAndReturnFile(file: GalleryMedia.File) {
        downloadMediaFileViewModel.downloadFile(
            file = file,
            destination = downloadMediaFileViewModel.getInternalDownloadDestination(
                downloadsDirectory = internalDownloadsDir,
            ),
            onSuccess = { destinationFile ->
                eventsSubject.onNext(
                    Event.ReturnDownloadedFiles(
                        SendableFile(
                            downloadedMediaFile = destinationFile,
                            mediaFile = file,
                        )
                    )
                )
            }
        )
    }

    private fun downloadMultipleSelectionFiles(
        intent: DownloadSelectedFilesIntent,
    ) {
        val filesAndDestinations =
            multipleSelectionFilesByMediaUid.values.mapIndexed { i, mediaFile ->
                when (intent) {
                    DownloadSelectedFilesIntent.DOWNLOAD_TO_EXTERNAL_STORAGE ->
                        mediaFile to downloadMediaFileViewModel.getExternalDownloadDestination(
                            downloadsDirectory = externalDownloadsDir,
                            file = mediaFile,
                        )

                    else ->
                        mediaFile to downloadMediaFileViewModel.getInternalDownloadDestination(
                            downloadsDirectory = internalDownloadsDir,
                            index = i,
                        )
                }
            }

        log.debug {
            "downloadMultipleSelectionFiles(): start_downloading_files:" +
                    "\nintent=$intent"
        }

        downloadMediaFileViewModel.downloadFiles(
            filesAndDestinations = filesAndDestinations,
            onSuccess = {
                val downloadedFiles = filesAndDestinations.map { (mediaFile, destination) ->
                    SendableFile(
                        downloadedMediaFile = destination,
                        mediaFile = mediaFile,
                    )
                }

                when (intent) {
                    DownloadSelectedFilesIntent.RETURN -> {
                        log.debug {
                            "downloadMultipleSelectionFiles(): returning_files:" +
                                    "\ndownloadedFiles=${downloadedFiles.size}"
                        }

                        eventsSubject.onNext(Event.ReturnDownloadedFiles(downloadedFiles))
                    }

                    DownloadSelectedFilesIntent.SHARE -> {
                        log.debug {
                            "downloadMultipleSelectionFiles(): sharing_files:" +
                                    "\ndownloadedFiles=${downloadedFiles.size}"
                        }

                        eventsSubject.onNext(Event.ShareDownloadedFiles(downloadedFiles))
                    }

                    DownloadSelectedFilesIntent.DOWNLOAD_TO_EXTERNAL_STORAGE -> {
                        eventsSubject.onNext(Event.ShowFilesDownloadedMessage)

                        switchToViewing()
                    }
                }
            }
        )
    }

    private fun openViewer(
        media: GalleryMedia,
        areActionsEnabled: Boolean,
    ) {
        val currentMediaRepository = this.currentMediaRepository
            ?: return

        val index = currentMediaRepository.itemsList.indexOf(media)
        val repositoryParams = currentMediaRepository.params

        log.debug {
            "openViewer(): opening_viewer:" +
                    "\nmedia=$media," +
                    "\nindex=$index," +
                    "\nrepositoryParams=$repositoryParams," +
                    "\nareActionsEnabled=$areActionsEnabled"
        }

        eventsSubject.onNext(
            Event.OpenViewer(
                mediaIndex = index,
                repositoryParams = repositoryParams,
                areActionsEnabled = areActionsEnabled,
            )
        )
    }

    fun onViewerReturnedLastViewedMediaIndex(lastViewedMediaIndex: Int) {
        // Find the media list item index considering there are other item types.
        var mediaListItemIndex = -1
        var listItemIndex = 0
        var mediaItemsCounter = 0
        for (item in itemsList.value ?: emptyList()) {
            if (item is GalleryListItem.Media) {
                mediaItemsCounter++
            }
            if (mediaItemsCounter == lastViewedMediaIndex + 1) {
                mediaListItemIndex = listItemIndex
                break
            }
            listItemIndex++
        }

        // Ensure that the last viewed media is visible in the gallery list.
        if (mediaListItemIndex >= 0) {
            log.debug {
                "onViewerReturnedLastViewedMediaIndex(): ensuring_media_list_item_visibility:" +
                        "\nmediaIndex=$lastViewedMediaIndex" +
                        "\nmediaListItemIndex=$mediaListItemIndex"
            }

            eventsSubject.onNext(
                Event.EnsureListItemVisible(
                    listItemIndex = mediaListItemIndex,
                )
            )
        } else {
            log.error {
                "onViewerReturnedLastViewedMediaIndex(): cant_find_media_list_item_index:" +
                        "\nmediaIndex=$lastViewedMediaIndex"
            }
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

    fun onPreferencesButtonClicked() {
        eventsSubject.onNext(Event.OpenPreferences)
    }

    fun onClearMultipleSelectionClicked() {
        val currentState = stateSubject.value

        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Clear multiple selection button is only clickable in the corresponding state"
        }

        when (currentState) {
            is State.Selecting.ForOtherApp -> {
                log.debug { "onClearMultipleSelectionClicked(): clearing_selection" }

                clearMultipleSelection()
            }

            State.Selecting.ForUser -> {
                log.debug { "onClearMultipleSelectionClicked(): switching_to_viewing" }

                switchToViewing()
            }
        }
    }

    private fun clearMultipleSelection() {
        multipleSelectionFilesByMediaUid.clear()
        postGalleryItems()
        postMultipleSelectionItemsCount()
    }

    fun onDoneMultipleSelectionClicked() {
        val currentState = stateSubject.value
        check(currentState is State.Selecting.ForOtherApp && currentState.allowMultiple) {
            "Done multiple selection button is only clickable when selecting multiple for other app"
        }

        check(multipleSelectionFilesByMediaUid.isNotEmpty()) {
            "Done multiple selection button is only clickable when something is selected"
        }

        downloadMultipleSelectionFiles(
            intent = DownloadSelectedFilesIntent.RETURN,
        )
    }

    fun onShareMultipleSelectionClicked() {
        val currentState = stateSubject.value
        check(currentState is State.Selecting.ForUser) {
            "Share multiple selection button is only clickable when selecting"
        }

        check(multipleSelectionFilesByMediaUid.isNotEmpty()) {
            "Share multiple selection button is only clickable when something is selected"
        }

        downloadMultipleSelectionFiles(
            intent = DownloadSelectedFilesIntent.SHARE,
        )
    }

    fun onDownloadMultipleSelectionClicked() {
        val currentState = stateSubject.value
        check(currentState is State.Selecting.ForUser) {
            "Download multiple selection button is only clickable when selecting"
        }

        check(multipleSelectionFilesByMediaUid.isNotEmpty()) {
            "Download multiple selection button is only clickable when something is selected"
        }

        if (downloadMediaFileViewModel.isExternalDownloadStoragePermissionRequired) {
            log.debug {
                "onDownloadMultipleSelectionClicked(): must_check_storage_permission"
            }

            eventsSubject.onNext(Event.CheckStoragePermission)
        } else {
            log.debug {
                "onDownloadMultipleSelectionClicked(): no_need_to_check_storage_permission"
            }

            downloadMultipleSelectionFiles(
                intent = DownloadSelectedFilesIntent.DOWNLOAD_TO_EXTERNAL_STORAGE,
            )
        }
    }

    fun onStoragePermissionResult(isGranted: Boolean) {
        log.debug {
            "onStoragePermissionResult(): received_result:" +
                    "\nisGranted=$isGranted"
        }

        when (val state = stateSubject.value!!) {
            is State.Selecting.ForUser ->
                if (isGranted) {
                    downloadMultipleSelectionFiles(
                        intent = DownloadSelectedFilesIntent.DOWNLOAD_TO_EXTERNAL_STORAGE,
                    )
                } else {
                    eventsSubject.onNext(Event.ShowMissingStoragePermissionMessage)
                }

            else ->
                error("Can't handle storage permission in $state state")
        }
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
                    eventsSubject.onNext(Event.GoToEnvConnection)
                },
                onError = { error ->
                    log.error(error) {
                        "disconnect(): error_occurred"
                    }

                    eventsSubject.onNext(
                        Event.ShowFloatingError(
                            Error.LoadingFailed(
                                shortSummary = error.shortSummary
                            )
                        )
                    )
                }
            )
            .autoDispose(this)
    }

    private fun switchToViewing() {
        assert(stateSubject.value is State.Selecting.ForUser) {
            "Switching to viewing is only possible while selecting to share"
        }

        backPressActionsStack.removeAction(switchBackToViewingOnBackPress)
        stateSubject.onNext(State.Viewing)

        clearMultipleSelection()
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
         * Open the media file selection dialog.
         *
         * Once selected, the [onFileSelected] method should be called.
         */
        class OpenFileSelectionDialog(val files: List<GalleryMedia.File>) : Event

        /**
         * Return the files to the requesting app when the selection is done.
         */
        class ReturnDownloadedFiles(
            val files: List<SendableFile>,
        ) : Event {
            constructor(downloadedFile: SendableFile) : this(listOf(downloadedFile))
        }

        /**
         * Share the files with any app of the user's choice when the selection is done.
         *
         * Once shared, the [onDownloadedFilesShared] method should be called.
         */
        class ShareDownloadedFiles(
            val files: List<SendableFile>,
        ) : Event

        class OpenViewer(
            val mediaIndex: Int,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
            val areActionsEnabled: Boolean,
        ) : Event

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
        class ShowFloatingError(val error: Error) : Event

        object OpenPreferences : Event

        /**
         * Ensure that the given item of the [itemsList] is visible on the screen.
         */
        class EnsureListItemVisible(val listItemIndex: Int) : Event

        /**
         * Close the screen and go to the connection.
         */
        object GoToEnvConnection : Event

        object ShowFilesDownloadedMessage : Event

        /**
         * Check the external storage write permission reporting the result
         * to the [onStoragePermissionResult] method.
         */
        object CheckStoragePermission : Event

        object ShowMissingStoragePermissionMessage : Event
    }

    sealed interface Error {
        /**
         * Can't establish a connection with the library
         */
        object LibraryNotAccessible : Error

        /**
         * The data has been requested, but something went wrong
         * while receiving the response.
         */
        class LoadingFailed(val shortSummary: String) : Error

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

        /**
         * Automatic session renewal failed because the credentials
         * have been changed. Disconnect is required.
         */
        object CredentialsHaveBeenChanged : Error
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
         * Change caused by something else.
         */
        class Other(repository: SimpleGalleryMediaRepository) :
            MediaRepositoryChange(repository)
    }

    private enum class DownloadSelectedFilesIntent {
        RETURN,
        SHARE,
        DOWNLOAD_TO_EXTERNAL_STORAGE,
        ;
    }
}
