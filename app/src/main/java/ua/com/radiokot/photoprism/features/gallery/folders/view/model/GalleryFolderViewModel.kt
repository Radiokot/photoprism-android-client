package ua.com.radiokot.photoprism.features.gallery.folders.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.ArchiveGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.DeleteGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadSelectedFilesIntent
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import ua.com.radiokot.photoprism.util.BackPressActionsStack
import java.io.File
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class GalleryFolderViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val internalDownloadsDir: File,
    private val externalDownloadsDir: File,
    private val galleryPreferences: GalleryPreferences,
    private val archiveGalleryMediaUseCase: ArchiveGalleryMediaUseCase,
    private val deleteGalleryMediaUseCase: DeleteGalleryMediaUseCase,
    val downloadMediaFileViewModel: DownloadMediaFileViewModel,
) : ViewModel() {
    // TODO: refactor to eliminate duplication.

    private val log = kLogger("GalleryFolderAVM")
    private val galleryItemsPostingSubject = PublishSubject.create<SimpleGalleryMediaRepository>()
    private var isInitialized = false
    private lateinit var currentMediaRepository: SimpleGalleryMediaRepository
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<GalleryListItem>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.toMainThreadObservable()
    private val stateSubject = BehaviorSubject.create<State>()
    val state: Observable<State> = stateSubject.toMainThreadObservable()
    val currentState: State
        get() = stateSubject.value!!
    val mainError = MutableLiveData<Error?>(null)
    var canLoadMore = true
        private set
    private val multipleSelectionFilesByMediaUid = linkedMapOf<String, GalleryMedia.File>()
    val multipleSelectionItemsCount: MutableLiveData<Int> = MutableLiveData(0)
    val itemScale: MutableLiveData<GalleryItemScale> =
        MutableLiveData(galleryPreferences.itemScale.value!!)

    private val backPressActionsStack = BackPressActionsStack()
    val backPressedCallback: OnBackPressedCallback =
        backPressActionsStack.onBackPressedCallback
    private val switchBackToViewingOnBackPress = {
        switchToViewing()
    }

    fun initSelectionForAppOnce(
        repositoryParams: SimpleGalleryMediaRepository.Params,
        allowMultiple: Boolean,
    ) {
        if (isInitialized) {
            log.debug {
                "initSelectionForAppOnce(): already_initialized"
            }

            return
        }

        currentMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)

        log.debug {
            "initSelectionForAppOnce(): initialized_selection:" +
                    "\nrepositoryParams=$repositoryParams," +
                    "\nallowMultiple=$allowMultiple"
        }

        stateSubject.onNext(
            State.Selecting.ForOtherApp(
                allowMultiple = allowMultiple,
            )
        )

        initCommon()

        isInitialized = true
    }

    fun initViewingOnce(
        repositoryParams: SimpleGalleryMediaRepository.Params,
    ) {
        if (isInitialized) {
            log.debug {
                "initViewingOnce(): already_initialized"
            }

            return
        }

        currentMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)

        log.debug {
            "initViewingOnce(): initialized_viewing"
        }

        stateSubject.onNext(State.Viewing)

        initCommon()

        isInitialized = true
    }

    private fun initCommon() {
        subscribeGalleryItemsPosting()
        subscribeToRepository()
        subscribeToPreferences()

        update()
    }

    private fun subscribeToRepository() {
        log.debug {
            "subscribeToRepository(): subscribing:" +
                    "\nrepository=$currentMediaRepository"
        }

        currentMediaRepository.items
            .subscribe { postGalleryItemsAsync() }
            .autoDispose(this)

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
            .autoDispose(this)

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
            .autoDispose(this)
    }

    private fun subscribeToPreferences() {
        galleryPreferences.itemScale
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { newItemScale ->
                if (newItemScale != itemScale.value) {
                    itemScale.value = newItemScale
                    val postItems = itemsList.value != null

                    log.debug {
                        "subscribeToPreferences(): item_scale_changed:" +
                                "\nnewItemScale=$newItemScale," +
                                "\npostItems=$postItems"
                    }

                    if (postItems) {
                        postGalleryItemsAsync()
                    }
                }
            }
            .autoDispose(this)
    }

    private fun subscribeGalleryItemsPosting() =
        galleryItemsPostingSubject
            .observeOn(Schedulers.computation())
            // Post empty lists immediately for better visual,
            // therefore do not proceed further.
            .filter { repository ->
                if (repository.itemsList.isEmpty()) {
                    postGalleryItems(repository)
                    false
                } else {
                    true
                }
            }
            // Small debounce is nice for situations when multiple changes
            // trigger items posting, e.g. state and repository.
            .debounce(30, TimeUnit.MILLISECONDS)
            // Proceed only if the repository remained the same.
            .filter { it == currentMediaRepository }
            .subscribe(::postGalleryItems)
            .autoDispose(this)

    /**
     * Schedules preparation and posting the items in a non-main thread.
     *
     * @see subscribeGalleryItemsPosting
     */
    private fun postGalleryItemsAsync() {
        val repository = currentMediaRepository.checkNotNull {
            "There must be a media repository to post items from"
        }
        galleryItemsPostingSubject.onNext(repository)
    }

    private fun postGalleryItems(repository: SimpleGalleryMediaRepository) {
        val galleryMediaList = repository.itemsList
        val itemScale = itemScale.value.checkNotNull {
            "There must be an item scale to consider"
        }

        mainError.postValue(
            when {
                galleryMediaList.isEmpty() && !repository.isNeverUpdated ->
                    Error.NoMediaFound

                else ->
                    // Dismiss the main error when there are items.
                    null
            }
        )

        val currentState = this.currentState
        val areViewButtonsVisible = currentState is State.Selecting
        val areSelectionViewsVisible = currentState is State.Selecting && currentState.allowMultiple

        val newListItems = galleryMediaList.mapIndexed { _, galleryMedia ->
            GalleryListItem.Media(
                source = galleryMedia,
                isViewButtonVisible = areViewButtonsVisible,
                isSelectionViewVisible = areSelectionViewsVisible,
                isMediaSelected = multipleSelectionFilesByMediaUid.containsKey(galleryMedia.uid),
                itemScale = itemScale,
            )
        }

        itemsList.postValue(newListItems)
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            currentMediaRepository.updateIfNotFresh()
        } else {
            currentMediaRepository.update()
            eventsSubject.onNext(Event.ResetScroll)

        }
    }

    fun loadMore() {
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

        when (val state = currentState) {
            is State.Selecting -> {
                if (state.allowMultiple) {
                    toggleMediaMultipleSelection(media)
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

        when (currentState) {
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
        assert(currentState is State.Viewing) {
            "Switching to selecting is only possible while viewing"
        }

        stateSubject.onNext(State.Selecting.ForUser)

        // Automatically select the target media.
        toggleMediaMultipleSelection(target)

        postGalleryItemsAsync()
        postMultipleSelectionItemsCount()

        // Make the back button press switch back to viewing.
        backPressActionsStack.pushUniqueAction(switchBackToViewingOnBackPress)
    }

    private fun selectMedia(media: GalleryMedia) {
        assert(currentState is State.Selecting) {
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
        assert(currentState is State.Selecting) {
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
        val currentState = this.currentState
        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Media file can only be added to the multiple selection in the corresponding state"
        }

        multipleSelectionFilesByMediaUid[file.mediaUid] = file

        log.debug {
            "addFileToMultipleSelection(): file_added:" +
                    "\nfile=$file," +
                    "\nmediaUid=${file.mediaUid}"
        }

        postGalleryItemsAsync()
        postMultipleSelectionItemsCount()
    }

    private fun removeMediaFromMultipleSelection(mediaUid: String) {
        val currentState = this.currentState
        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Media can only be removed from the multiple selection in the corresponding state"
        }

        multipleSelectionFilesByMediaUid.remove(mediaUid)

        log.debug {
            "removeMediaFromMultipleSelection(): media_removed:" +
                    "\nmediaUid=$mediaUid"
        }

        // If the last selected item has been removed, automatically switch back to viewing.
        // But only if was selecting for user.
        if (multipleSelectionFilesByMediaUid.isEmpty()
            && currentState is State.Selecting.ForUser
        ) {
            log.debug { "removeMediaFromMultipleSelection(): unselected_last_switching_to_viewing" }

            switchToViewing()
        }

        postGalleryItemsAsync()
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
        val currentState = this.currentState
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

    fun onClearMultipleSelectionClicked() {
        val currentState = this.currentState

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
        postGalleryItemsAsync()
        postMultipleSelectionItemsCount()
    }

    fun onDoneMultipleSelectionClicked() {
        val currentState = this.currentState
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
        check(currentState is State.Selecting.ForUser) {
            "Download multiple selection button is only clickable when selecting"
        }

        check(multipleSelectionFilesByMediaUid.isNotEmpty()) {
            "Download multiple selection button is only clickable when something is selected"
        }

        if (downloadMediaFileViewModel.isExternalDownloadStoragePermissionRequired) {
            log.debug {
                "onDownloadMultipleSelectionClicked(): must_request_storage_permission"
            }

            eventsSubject.onNext(Event.RequestStoragePermission)
        } else {
            log.debug {
                "onDownloadMultipleSelectionClicked(): no_need_to_check_storage_permission"
            }

            downloadMultipleSelectionFiles(
                intent = DownloadSelectedFilesIntent.DOWNLOAD_TO_EXTERNAL_STORAGE,
            )
        }
    }

    fun onArchiveMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Archive multiple selection button is only clickable when selecting"
        }

        check(multipleSelectionFilesByMediaUid.isNotEmpty()) {
            "Archive multiple selection button is only clickable when something is selected"
        }

        val repository = currentMediaRepository.checkNotNull {
            "There must be a media repository to archive items from"
        }

        val mediaUids = multipleSelectionFilesByMediaUid.keys

        archiveGalleryMediaUseCase
            .invoke(
                mediaUids = mediaUids,
                currentGalleryMediaRepository = repository,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                log.debug {
                    "onArchiveMultipleSelectionClicked(): start_archiving:" +
                            "\nitems=${mediaUids.size}"
                }
            }
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "onArchiveMultipleSelectionClicked(): failed_archiving"
                    }
                },
                onComplete = {
                    log.debug {
                        "onArchiveMultipleSelectionClicked(): successfully_archived"
                    }

                    switchToViewing()
                }
            )
            .autoDispose(this)
    }

    fun onDeleteMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Delete multiple selection button is only clickable when selecting"
        }

        check(multipleSelectionFilesByMediaUid.isNotEmpty()) {
            "Delete multiple selection button is only clickable when something is selected"
        }

        eventsSubject.onNext(Event.OpenDeletingConfirmationDialog)
    }

    fun onDeletingMultipleSelectionConfirmed() {
        val repository = currentMediaRepository.checkNotNull {
            "There must be a media repository to delete items from"
        }

        val mediaUids = multipleSelectionFilesByMediaUid.keys

        deleteGalleryMediaUseCase
            .invoke(
                mediaUids = mediaUids,
                currentGalleryMediaRepository = repository,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                log.debug {
                    "onDeletingMultipleSelectionConfirmed(): start_deleting:" +
                            "\nitems=${mediaUids.size}"
                }
            }
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "onDeletingMultipleSelectionConfirmed(): failed_deleting"
                    }
                },
                onComplete = {
                    log.debug {
                        "onDeletingMultipleSelectionConfirmed(): successfully_deleted"
                    }

                    switchToViewing()
                }
            )
            .autoDispose(this)
    }

    fun onStoragePermissionResult(isGranted: Boolean) {
        log.debug {
            "onStoragePermissionResult(): received_result:" +
                    "\nisGranted=$isGranted"
        }

        when (val state = currentState) {
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

    private fun switchToViewing() {
        assert(currentState is State.Selecting.ForUser) {
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
                allowMultiple: Boolean,
            ) : Selecting(
                allowMultiple = allowMultiple,
            )

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


        /**
         * Reset the scroll (to the top) and the infinite scrolling.
         */
        object ResetScroll : Event

        class OpenViewer(
            val mediaIndex: Int,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
            val areActionsEnabled: Boolean,
        ) : Event

        /**
         * Show a dismissible floating error.
         *
         * The [onFloatingErrorRetryClicked] method should be called
         * if the error assumes retrying.
         */
        class ShowFloatingError(val error: Error) : Event

        /**
         * Ensure that the given item of the [itemsList] is visible on the screen.
         */
        class EnsureListItemVisible(val listItemIndex: Int) : Event

        object ShowFilesDownloadedMessage : Event

        /**
         * Request the external storage write permission reporting the result
         * to the [onStoragePermissionResult] method.
         */
        object RequestStoragePermission : Event

        object ShowMissingStoragePermissionMessage : Event

        /**
         * Show item deletion confirmation, reporting the choice
         * to the [onDeletingMultipleSelectionConfirmed] method.
         */
        object OpenDeletingConfirmationDialog : Event
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
    }
}
