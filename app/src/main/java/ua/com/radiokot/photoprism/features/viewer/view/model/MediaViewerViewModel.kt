package ua.com.radiokot.photoprism.features.viewer.view.model

import android.util.Size
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.ArchiveGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.DeleteGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryContentLoadingError
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileDownloadActionsViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileDownloadActionsViewModelDelegate
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager
import ua.com.radiokot.photoprism.features.viewer.logic.UpdateGalleryMediaAttributesUseCase
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MediaViewerViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val updateGalleryMediaAttributesUseCase: UpdateGalleryMediaAttributesUseCase,
    private val archiveGalleryMediaUseCase: ArchiveGalleryMediaUseCase,
    private val deleteGalleryMediaUseCase: DeleteGalleryMediaUseCase,
    private val mediaFilesActionsViewModel: MediaFileDownloadActionsViewModelDelegate,
    private val galleryPreferences: GalleryPreferences,
) : ViewModel(),
    MediaFileDownloadActionsViewModel by mediaFilesActionsViewModel {

    private val log = kLogger("MediaViewerVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository
    private var isInitialized = false
    private var areActionsEnabled = false
    private var isPageIndicatorEnabled = false
    private var staticSubtitle: String? = null
    private val currentLocalDate = LocalDate()
    private var fileSelectionIntent: FileSelectionIntent? = null
    private var itemToDelete: GalleryMedia? = null

    // Media that turned to be not viewable.
    private val afterAllNotViewableMedia = mutableSetOf<GalleryMedia>()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPage>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOnMain()
    val areActionsVisible: MutableLiveData<Boolean> = MutableLiveData(true)
    val isPageIndicatorVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isToolbarVisible: MutableLiveData<Boolean> = MutableLiveData(true)
    val isFullScreen: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isCancelDownloadButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadCompletedIconVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val title: MutableLiveData<String> = MutableLiveData()
    val subtitle: MutableLiveData<SubtitleValue> = MutableLiveData()
    val isFavorite: MutableLiveData<Boolean> = MutableLiveData()
    val isPrivate: MutableLiveData<Boolean> = MutableLiveData()

    /**
     * Size of the image viewing area in px.
     * Used to load previews of suitable quality.
     * Must be set by the view.
     */
    var imageViewSize: Size = Size(0, 0)
        set(value) {
            val previous = field
            field = value
            if (previous != value) {
                log.debug {
                    "imageViewSize::set(): broadcasting_after_value_change:" +
                            "\nvalue=$value"
                }

                broadcastItemsFromRepository()
            }
        }

    /**
     * From 0 to 100, negative for indeterminate.
     */
    val cancelDownloadButtonProgressPercent: MutableLiveData<Int> = MutableLiveData(-1)

    init {
        // Make download button visibility opposite to the cancel download button.
        isCancelDownloadButtonVisible.observeForever { isCancelDownloadButtonVisible ->
            isDownloadButtonVisible.value = !isCancelDownloadButtonVisible
        }
    }

    fun initOnce(
        repositoryParams: SimpleGalleryMediaRepository.Params,
        areActionsEnabled: Boolean,
        isPageIndicatorEnabled: Boolean,
        staticSubtitle: String?,
    ) {
        if (isInitialized) {
            return
        }

        galleryMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)
            .checkNotNull {
                "The repository must be created beforehand"
            }
        subscribeToRepository()

        this.areActionsEnabled = areActionsEnabled
        this.isPageIndicatorEnabled = isPageIndicatorEnabled
        this.staticSubtitle = staticSubtitle

        initControlsVisibility()

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\nrepositoryParam=$repositoryParams," +
                    "\nareActionsEnabled=$areActionsEnabled," +
                    "\nisPageIndicatorEnabled=$isPageIndicatorEnabled," +
                    "\nstaticSubtitle=$staticSubtitle"
        }

        update()
    }

    private fun initControlsVisibility() {
        fun updateControlsVisibility(isFullScreen: Boolean) {
            areActionsVisible.value = !isFullScreen && areActionsEnabled
            isToolbarVisible.value = !isFullScreen
            isPageIndicatorVisible.value = !isFullScreen && isPageIndicatorEnabled
        }
        isFullScreen.observeForever(::updateControlsVisibility)
    }

    private fun subscribeToRepository() {
        galleryMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.isEmpty() && galleryMediaRepository.isFresh) {
                    log.debug {
                        "subscribeToRepository(): finishing_as_nothing_left"
                    }

                    eventsSubject.onNext(Event.Finish)
                } else {
                    broadcastItemsFromRepository()
                }
            }
            .autoDispose(this)

        galleryMediaRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(isLoading::setValue)
            .autoDispose(this)

        galleryMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) { "subscribeToRepository(): error_occurred" }

                val contentLoadingError = GalleryContentLoadingError.from(error)
                eventsSubject.onNext(Event.ShowFloatingError(contentLoadingError))
            }
            .autoDispose(this)
    }

    private fun broadcastItemsFromRepository() {
        if (imageViewSize.width == 0 || imageViewSize.height == 0) {
            log.warn {
                "broadcastItemsFromRepository(): broadcasting_null_without_image_view_size"
            }

            // When the image view size is unknown, new items can't be broadcast
            // while the existing are outdated.
            itemsList.value = null
            return
        }

        itemsList.value = galleryMediaRepository
            .itemsList
            .map { galleryMedia ->
                if (galleryMedia in afterAllNotViewableMedia)
                    MediaViewerPage.unsupported(galleryMedia)
                else
                    MediaViewerPage.fromGalleryMedia(
                        source = galleryMedia,
                        imageViewSize = imageViewSize,
                        livePhotosAsImages = galleryPreferences.livePhotosAsImages.value!!,
                    )
            }
            .also {
                log.debug {
                    "broadcastItemsFromRepository(): broadcasting:" +
                            "\nitemsCount=${it.size}"
                }
            }
    }

    private fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            galleryMediaRepository.update()
        } else {
            galleryMediaRepository.updateIfNotFresh()
        }
    }

    fun loadMore() {
        if (!galleryMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }
            galleryMediaRepository.loadMore()
        }
    }

    fun onShareClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onShareClicked(): position_out_of_range"
            }
            return
        }

        if (item.files.size > 1) {
            fileSelectionIntent = FileSelectionIntent.SHARING
            openFileSelectionDialog(item.files)
        } else {
            downloadAndShareFile(item.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    fun onStartSlideshowClicked(position: Int) {
        log.debug {
            "onStartSlideshowClicked(): starting_slideshow:" +
                    "\nposition=$position"
        }

        eventsSubject.onNext(
            Event.OpenSlideshow(
                mediaIndex = position,
                repositoryParams = galleryMediaRepository.params,
            )
        )
    }

    fun onOpenInClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onOpenInClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onOpenInClicked(): start_opening:" +
                    "\nitem=$item"
        }

        if (item.files.size > 1) {
            fileSelectionIntent = FileSelectionIntent.OPENING_IN
            openFileSelectionDialog(item.files)
        } else {
            downloadAndOpenFile(item.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    fun onOpenInWebViewerClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onOpenInWebViewerClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onOpenInWebViewerClicked(): opening_viewer:" +
                    "\nitem=$item"
        }

        eventsSubject.onNext(Event.OpenWebViewer(url = item.webViewUrl))
    }

    fun onArchiveClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onArchiveClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onArchiveClicked(): archiving:" +
                    "\nitem=$item"
        }

        archiveGalleryMediaUseCase
            .invoke(
                mediaUid = item.uid,
                currentGalleryMediaRepository = galleryMediaRepository,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "onArchiveClicked(): failed_archiving:" +
                                "\nitem=$item"
                    }
                },
                onComplete = {
                    log.debug {
                        "onArchiveClicked(): successfully_archived:" +
                                "\nitem=$item"
                    }
                }
            )
            .autoDispose(this)
    }

    fun onDeleteClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onDeleteClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onDeleteClicked(): start_deleting:" +
                    "\nitem=$item"
        }

        itemToDelete = item
        eventsSubject.onNext(Event.OpenDeletingConfirmationDialog)
    }

    fun onDeletingConfirmed() {
        val itemToDelete = itemToDelete.checkNotNull {
            "Confirming deletion when there's no item to delete"
        }

        log.debug {
            "onDeletingConfirmed(): deleting:" +
                    "\nitem=$itemToDelete"
        }

        deleteGalleryMediaUseCase
            .invoke(
                mediaUid = itemToDelete.uid,
                currentGalleryMediaRepository = galleryMediaRepository,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "onDeletingConfirmed(): failed_deleting:" +
                                "\nitem=$itemToDelete"
                    }
                },
                onComplete = {
                    log.debug {
                        "onDeletingConfirmed(): successfully_deleted:" +
                                "\nitem=$itemToDelete"
                    }
                }
            )
            .autoDispose(this)
    }

    fun onDownloadClicked(position: Int) {
        check(isDownloadButtonVisible.value == true) {
            "The button can't be clicked while it is not visible"
        }

        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onDownloadClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onDownloadClicked(): start_downloading_to_external_storage:" +
                    "\nitem=$item"
        }

        if (item.files.size > 1) {
            fileSelectionIntent = FileSelectionIntent.DOWNLOADING
            openFileSelectionDialog(item.files)
        } else {
            downloadFileToExternalStorageInBackground(item.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    fun onCancelDownloadClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onCancelDownloadClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onCancelDownloadClicked(): canceling_download"
        }

        mediaFilesActionsViewModel.cancelMediaFileBackgroundDownload(item.uid)
        unsubscribeFromDownloadProgress()
    }

    fun onFavoriteClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onFavoriteClicked(): position_out_of_range"
            }
            return
        }

        // Switch currently shown favorite state.
        val toSetFavorite = isFavorite.value != true

        log.debug {
            "onFavoriteClicked(): switching_favorite:" +
                    "\nitem=$item," +
                    "\ntoSetFavorite=$toSetFavorite"
        }

        updateGalleryMediaAttributesUseCase
            .invoke(
                mediaUid = item.uid,
                isFavorite = toSetFavorite,
                currentGalleryMediaRepository = galleryMediaRepository,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                // Change the value immediately for pleasant user experience.
                isFavorite.value = toSetFavorite
            }
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "onFavoriteClicked(): failed_switching_favorite:" +
                                "\nitem=$item," +
                                "\ntoSetFavorite=$toSetFavorite"
                    }
                },
                onComplete = {
                    log.debug {
                        "onFavoriteClicked(): successfully_switched_favorite:" +
                                "\nitem=$item," +
                                "\ntoSetFavorite=$toSetFavorite"
                    }
                }
            )
            .autoDispose(this)
    }

    fun onPrivateClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onPrivateClicked(): position_out_of_range"
            }
            return
        }

        // Switch currently shown private state.
        val toSetPrivate = isPrivate.value != true

        log.debug {
            "onPrivateClicked(): switching_private:" +
                    "\nitem=$item," +
                    "\ntoSetPrivate=$toSetPrivate"
        }

        updateGalleryMediaAttributesUseCase
            .invoke(
                mediaUid = item.uid,
                isPrivate = toSetPrivate,
                currentGalleryMediaRepository = galleryMediaRepository,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "onPrivateClicked(): failed_switching_private:" +
                                "\nitem=$item," +
                                "\ntoSetPrivate=$toSetPrivate"
                    }
                },
                onComplete = {
                    log.debug {
                        "onPrivateClicked(): successfully_switched_private:" +
                                "\nitem=$item," +
                                "\ntoSetPrivate=$toSetPrivate"
                    }

                    isPrivate.value = toSetPrivate
                }
            )
            .autoDispose(this)
    }

    fun onPageClicked() {
        log.debug { "onPageClicked(): toggling_full_screen" }

        isFullScreen.value = !isFullScreen.value!!
    }

    fun onFullScreenToggledBySystem(isFullScreen: Boolean) {
        if (isFullScreen != this.isFullScreen.value) {
            log.debug {
                "onFullScreenToggledBySystem(): system_toggled_full_screen:" +
                        "\nisFullScreen=$isFullScreen"
            }

            this.isFullScreen.value = isFullScreen
        }
    }

    private fun openFileSelectionDialog(files: List<GalleryMedia.File>) {
        log.debug {
            "openFileSelectionDialog(): posting_open_event:" +
                    "\nfiles=$files"
        }

        eventsSubject.onNext(Event.OpenFileSelectionDialog(files))
    }

    fun onFileSelected(file: GalleryMedia.File) {
        log.debug {
            "onFileSelected(): file_selected:" +
                    "\nfile=$file"
        }

        val intent = fileSelectionIntent.checkNotNull {
            "File is selected when there's no intent"
        }

        when (intent) {
            FileSelectionIntent.SHARING ->
                downloadAndShareFile(file)

            FileSelectionIntent.OPENING_IN ->
                downloadAndOpenFile(file)

            FileSelectionIntent.DOWNLOADING ->
                downloadFileToExternalStorageInBackground(file)
        }
    }

    fun onVideoPlayerFatalPlaybackError(page: MediaViewerPage) {
        val source = page.source

        log.error {
            "onVideoPlayerFatalPlaybackError(): error_occurred:" +
                    "\nsourceMedia=$source"
        }

        if (source != null) {
            afterAllNotViewableMedia.add(source)
            log.debug {
                "onVideoPlayerFatalPlaybackError(): added_media_to_not_viewable:" +
                        "\nmedia=$source"
            }
            broadcastItemsFromRepository()
        }
    }

    private fun downloadAndShareFile(file: GalleryMedia.File) {
        mediaFilesActionsViewModel.downloadAndShareMediaFiles(
            files = listOf(file),
        )
    }

    private fun downloadAndOpenFile(file: GalleryMedia.File) {
        mediaFilesActionsViewModel.downloadAndOpenMediaFile(
            file = file,
        )
    }

    private fun downloadFileToExternalStorageInBackground(file: GalleryMedia.File) {
        mediaFilesActionsViewModel.downloadMediaFileToExternalStorageInBackground(
            file = file,
            onDownloadEnqueued = { sendableFile ->
                log.debug {
                    "downloadFileToExternalStorageInBackground(): enqueued_download:" +
                            "\nfile=$file," +
                            "\ndestination=${sendableFile.file}"
                }
                val progressObservable =
                    mediaFilesActionsViewModel.getMediaFileBackgroundDownloadStatus(
                        mediaUid = file.mediaUid,
                    )

                subscribeToMediaBackgroundDownloadStatus(progressObservable)

                eventsSubject.onNext(
                    Event.ShowStartedDownloadMessage(
                        destinationFileName = sendableFile.file.name,
                    )
                )
            }
        )
    }

    fun onPageChanged(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        log.debug {
            "onPageChanged(): page_changed:" +
                    "\nposition=$position," +
                    "\nitem=$item"
        }

        if (item == null) {
            log.warn {
                "onPageChanged(): position_out_of_range"
            }
            return
        }

        subscribeToMediaBackgroundDownloadStatus(
            statusObservable = mediaFilesActionsViewModel.getMediaFileBackgroundDownloadStatus(
                mediaUid = item.uid,
            )
        )
        updateTitleAndSubtitle(item)
        isFavorite.value = item.isFavorite
        isPrivate.value = item.isPrivate

        // When switching to a video (not live photo or GIF), go full screen if currently is not.
        if (item.media is GalleryMedia.TypeData.Video && isFullScreen.value == false) {
            isFullScreen.value = true
        }
    }

    private var backgroundDownloadProgressDisposable: Disposable? = null
    private fun subscribeToMediaBackgroundDownloadStatus(
        statusObservable: Observable<out BackgroundMediaFileDownloadManager.Status>,
    ) {
        val resetDownloadViews = {
            isCancelDownloadButtonVisible.value = false
            isDownloadCompletedIconVisible.value = false
        }

        backgroundDownloadProgressDisposable?.dispose()
        backgroundDownloadProgressDisposable = statusObservable
            // Here it is important to emit the last item,
            // as the subscription has no separate handler for Observable completion.
            .throttleLatest(500, TimeUnit.MILLISECONDS, true)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { resetDownloadViews() }
            .doOnDispose(resetDownloadViews)
            .subscribeBy(
                onNext = { status ->
                    isCancelDownloadButtonVisible.value =
                        status is BackgroundMediaFileDownloadManager.Status.InProgress
                    isDownloadCompletedIconVisible.value =
                        status is BackgroundMediaFileDownloadManager.Status.Ended.Completed

                    if (status is BackgroundMediaFileDownloadManager.Status.InProgress) {
                        cancelDownloadButtonProgressPercent.value =
                            if (status.percent < 0)
                                -1
                            else
                                status.percent.roundToInt().coerceAtLeast(1)
                    }
                },
            )
            .autoDispose(this)
    }

    private fun unsubscribeFromDownloadProgress() {
        backgroundDownloadProgressDisposable?.dispose()
    }

    private fun updateTitleAndSubtitle(item: GalleryMedia) {
        title.value = item.title

        val takenAtLocal = item.takenAtLocal
        val staticSubtitleValue = staticSubtitle
        subtitle.value =
            if (staticSubtitleValue != null)
                SubtitleValue.Static(staticSubtitleValue)
            else
                SubtitleValue.DateTime(
                    localDate = takenAtLocal,
                    withYear = !takenAtLocal.isSameYearAs(currentLocalDate),
                )
    }

    fun onFloatingErrorRetryClicked() {
        log.debug {
            "onFloatingErrorRetryClicked(): updating"
        }

        update(force = true)
    }

    sealed interface SubtitleValue {
        class Static(val value: String) : SubtitleValue

        class DateTime(
            val localDate: LocalDate,
            val withYear: Boolean,
        ) : SubtitleValue
    }

    sealed interface Event {
        class OpenFileSelectionDialog(val files: List<GalleryMedia.File>) : Event

        object RequestStoragePermission : Event
        object ShowMissingStoragePermissionMessage : Event
        class ShowStartedDownloadMessage(val destinationFileName: String) : Event
        class OpenWebViewer(val url: String) : Event
        class OpenSlideshow(
            val mediaIndex: Int,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
        ) : Event

        object Finish : Event

        /**
         * Show item deletion confirmation, reporting the choice
         * to the [onDeletingConfirmed] method.
         */
        object OpenDeletingConfirmationDialog : Event

        /**
         * Show a dismissible floating error.
         *
         * The [onFloatingErrorRetryClicked] method should be called
         * if the error assumes retrying.
         */
        @JvmInline
        value class ShowFloatingError(
            val error: GalleryContentLoadingError
        ) : Event
    }

    private enum class FileSelectionIntent {
        SHARING,
        OPENING_IN,
        DOWNLOADING,
        ;
    }
}
