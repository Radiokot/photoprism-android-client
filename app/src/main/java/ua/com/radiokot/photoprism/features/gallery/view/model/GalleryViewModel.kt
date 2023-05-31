package ua.com.radiokot.photoprism.features.gallery.view.model

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
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.isSameDayAs
import ua.com.radiokot.photoprism.extension.isSameMonthAs
import ua.com.radiokot.photoprism.extension.isSameYearAs
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import java.io.File
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class GalleryViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val dateHeaderDayYearDateFormat: DateFormat,
    private val dateHeaderDayDateFormat: DateFormat,
    private val dateHeaderMonthYearDateFormat: DateFormat,
    private val dateHeaderMonthDateFormat: DateFormat,
    private val internalDownloadsDir: File,
    val downloadMediaFileViewModel: DownloadMediaFileViewModel,
    val searchViewModel: GallerySearchViewModel,
    val fastScrollViewModel: GalleryFastScrollViewModel,
) : ViewModel() {
    private val log = kLogger("GalleryVM")
    private val mediaRepositoryChanges = BehaviorSubject.create<MediaRepositoryChange>()

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

    fun initSelectionOnce(
        requestedMimeType: String?,
        allowMultiple: Boolean,
    ) {
        if (isInitialized) {
            log.debug {
                "initSelection(): already_initialized"
            }

            return
        }

        val allowedMediaTypes: Set<GalleryMedia.TypeName>? = when {
            requestedMimeType == null ->
                null

            requestedMimeType.startsWith("image/") ->
                setOf(
                    GalleryMedia.TypeName.IMAGE,
                    GalleryMedia.TypeName.ANIMATED,
                    GalleryMedia.TypeName.VECTOR
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
            "initSelection(): initialized_selection:" +
                    "\nrequestedMimeType=$requestedMimeType," +
                    "\nallowMultiple=$allowMultiple," +
                    "\nallowedMediaTypes=$allowedMediaTypes"
        }

        stateSubject.onNext(
            State.Selecting(
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
                "initViewing(): already_initialized"
            }

            return
        }

        log.debug {
            "initViewing(): initialized_viewing"
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
        when (val state = stateSubject.value!!) {
            is State.Selecting -> {
                val searchConfig =
                    SearchConfig.DEFAULT
                        .withOnlyAllowedMediaTypes(state.allowedMediaTypes)

                currentSearchConfig = searchConfig

                mediaRepositoryChanges.onNext(
                    MediaRepositoryChange.Other(
                        galleryMediaRepositoryFactory.getForSearch(searchConfig),
                    )
                )
            }

            State.Viewing -> {
                currentSearchConfig = null
                mediaRepositoryChanges.onNext(
                    MediaRepositoryChange.Other(
                        galleryMediaRepositoryFactory.get(),
                    )
                )
            }
        }
    }

    private fun subscribeToSearch() {
        searchViewModel.state.subscribe { state ->
            log.debug {
                "subscribeToSearch(): received_new_state:" +
                        "\nstate=$state"
            }

            when (state) {
                is GallerySearchViewModel.State.AppliedSearch -> {
                    val currentState = stateSubject.value
                    val searchConfigToApply: SearchConfig =
                        if (currentState is State.Selecting) {
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

                    fastScrollViewModel.reset()

                    if (searchMediaRepository != mediaRepositoryChanges.value?.repository) {
                        mediaRepositoryChanges.onNext(
                            MediaRepositoryChange.Search(
                                searchMediaRepository
                            )
                        )
                    }
                }

                GallerySearchViewModel.State.NoSearch -> {
                    fastScrollViewModel.reset()
                    resetRepositoryToInitial()
                }

                is GallerySearchViewModel.State.ConfiguringSearch -> {
                    // Nothing to change.
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
                    val searchConfigForMonth: SearchConfig? =
                        if (event.isTopMonth)
                        // For top month we don't need to alter the "before" date,
                        // if altered the result is the same as if the date is not set at all.
                            currentSearchConfig
                        else
                            (currentSearchConfig ?: SearchConfig.DEFAULT)
                                .copy(before = event.month.nextDayAfter)

                    // Always reset the scroll.
                    // If the list is scrolled manually, setting fast scroll to the same month
                    // must bring it back to the top.
                    eventsSubject.onNext(Event.ResetScroll)

                    // We need to change the repo if scrolled to the top month
                    // (return to initial state before scrolling)
                    // or if the search config for the month differs from the current.
                    if (event.isTopMonth || searchConfigForMonth != currentSearchConfig) {
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
            }

            log.debug {
                "subscribeToFastScroll(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.autoDispose(this)
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
                    Error.SearchDoesntFitAllowedTypes

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
        val today = Date()
        galleryMediaList
            .forEachIndexed { i, galleryMedia ->
                val takenAt = galleryMedia.takenAt

                if (i == 0 && !takenAt.isSameMonthAs(today)
                    || i != 0 && !takenAt.isSameMonthAs(galleryMediaList[i - 1].takenAt)
                ) {
                    val formattedMonth =
                        if (takenAt.isSameYearAs(today))
                            dateHeaderMonthDateFormat.format(takenAt)
                        else
                            dateHeaderMonthYearDateFormat.format(takenAt)

                    newListItems.add(
                        GalleryListItem.Header.month(
                            text = formattedMonth,
                        )
                    )
                }

                if (i == 0 || !takenAt.isSameDayAs(galleryMediaList[i - 1].takenAt)) {
                    newListItems.add(
                        if (takenAt.isSameDayAs(today))
                            GalleryListItem.Header.day(
                                textRes = R.string.today,
                            )
                        else {
                            val formattedDate =
                                if (takenAt.isSameYearAs(today))
                                    dateHeaderDayDateFormat.format(takenAt)
                                else
                                    dateHeaderDayYearDateFormat.format(takenAt)

                            GalleryListItem.Header.day(
                                text = formattedDate.replaceFirstChar {
                                    if (it.isLowerCase())
                                        it.titlecase(Locale.getDefault())
                                    else
                                        it.toString()
                                },
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

        if (item !is GalleryListItem.Media) {
            return
        }

        when (val state = stateSubject.value.checkNotNull()) {
            is State.Selecting -> {
                val media = item.source
                    ?: return

                if (state.allowMultiple) {
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
                } else {
                    if (media.files.size > 1) {
                        openFileSelectionDialog(media.files)
                    } else {
                        downloadAndReturnFile(media.files.firstOrNull().checkNotNull {
                            "There must be at least one file in the gallery media object"
                        })
                    }
                }
            }

            is State.Viewing ->
                if (item.source != null) {
                    openViewer(
                        media = item.source,
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
            destination = File(internalDownloadsDir, "downloaded"),
            onSuccess = { destinationFile ->
                eventsSubject.onNext(
                    Event.ReturnDownloadedFiles(
                        Event.ReturnDownloadedFiles.FileToReturn(
                            downloadedFile = destinationFile,
                            mimeType = file.mimeType,
                            displayName = File(file.name).name
                        )
                    )
                )
            }
        )
    }

    private fun downloadAndReturnMultipleSelectionFiles() {
        val filesAndDestinations =
            multipleSelectionFilesByMediaUid.values.mapIndexed { i, mediaFile ->
                mediaFile to File(internalDownloadsDir, "downloaded_$i")
            }

        downloadMediaFileViewModel.downloadFiles(
            filesAndDestinations = filesAndDestinations,
            onSuccess = {
                log.debug { "downloadAndReturnMultipleSelectionFiles(): download_completed" }

                eventsSubject.onNext(
                    Event.ReturnDownloadedFiles(
                        filesAndDestinations.map { (mediaFile, destination) ->
                            Event.ReturnDownloadedFiles.FileToReturn(
                                downloadedFile = destination,
                                mimeType = mediaFile.mimeType,
                                displayName = File(mediaFile.name).name
                            )
                        }
                    )
                )
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
        log.debug { "onClearMultipleSelectionClicked(): clearing_selection" }

        multipleSelectionFilesByMediaUid.clear()
        postGalleryItems()
        postMultipleSelectionItemsCount()
    }

    fun onDoneMultipleSelectionClicked() {
        val currentState = stateSubject.value
        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Done multiple selection button is only clickable in the corresponding state"
        }

        check(multipleSelectionFilesByMediaUid.isNotEmpty()) {
            "Done multiple selection button is only clickable when something is selected"
        }

        log.debug {
            "onDoneMultipleSelectionClicked(): start_downloading_multiple_selection_files"
        }

        downloadAndReturnMultipleSelectionFiles()
    }

    sealed interface Event {
        class OpenFileSelectionDialog(val files: List<GalleryMedia.File>) : Event

        class ReturnDownloadedFiles(
            val files: List<FileToReturn>,
        ) : Event {
            constructor(fileToReturn: FileToReturn) : this(listOf(fileToReturn))

            data class FileToReturn(
                val downloadedFile: File,
                val mimeType: String,
                val displayName: String,
            )
        }

        class OpenViewer(
            val mediaIndex: Int,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
            val areActionsEnabled: Boolean,
        ) : Event

        object ResetScroll : Event

        class ShowFloatingError(val error: Error) : Event

        object OpenPreferences : Event

        class EnsureListItemVisible(val listItemIndex: Int) : Event
    }

    sealed interface State {
        object Viewing : State
        class Selecting(
            /**
             * Non-empty optional set of allowed media types (e.g. only images)
             */
            val allowedMediaTypes: Set<GalleryMedia.TypeName>?,
            /**
             * Whether selection of multiple items is allowed or not.
             */
            val allowMultiple: Boolean,
        ) : State {
            init {
                require(allowedMediaTypes == null || allowedMediaTypes.isNotEmpty()) {
                    "The set of allowed types must either be null or not empty"
                }
            }
        }
    }

    sealed interface Error {
        object LibraryNotAccessible : Error
        class LoadingFailed(val shortSummary: String) : Error
        object NoMediaFound : Error
        object SearchDoesntFitAllowedTypes : Error
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
}