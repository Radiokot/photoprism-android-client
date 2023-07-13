package ua.com.radiokot.photoprism.features.viewer.view.model

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import okhttp3.HttpUrl.Companion.toHttpUrl
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.capitalized
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.extension.withMaskedCredentials
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager
import ua.com.radiokot.photoprism.util.LocalDate
import java.io.File
import java.text.DateFormat
import kotlin.math.roundToInt

class MediaViewerViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val internalDownloadsDir: File,
    private val externalDownloadsDir: File,
    val downloadMediaFileViewModel: DownloadMediaFileViewModel,
    private val backgroundMediaFileDownloadManager: BackgroundMediaFileDownloadManager,
    private val utcDateTimeDateFormat: DateFormat,
    private val utcDateTimeYearDateFormat: DateFormat,
) : ViewModel() {
    private val log = kLogger("MediaViewerVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository
    private var isInitialized = false
    private var areActionsEnabled = false
    private val currentLocalDate = LocalDate()

    // Media that turned to be not viewable.
    private val afterAllNotViewableMedia = mutableSetOf<GalleryMedia>()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPage>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.toMainThreadObservable()
    private val stateSubject = BehaviorSubject.createDefault<State>(State.Idle)
    val state: Observable<State> = stateSubject.toMainThreadObservable()
    val areActionsVisible: MutableLiveData<Boolean> = MutableLiveData(true)
    val isToolbarVisible: MutableLiveData<Boolean> = MutableLiveData(true)
    val isFullScreen: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isCancelDownloadButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadCompletedIconVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val title: MutableLiveData<String> = MutableLiveData()
    val subtitle: MutableLiveData<String> = MutableLiveData()

    // From 0 to 100, negative for indeterminate.
    val cancelDownloadButtonProgressPercent: MutableLiveData<Int> = MutableLiveData(-1)

    private val isStoragePermissionNeeded =
        Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q)

    init {
        // Make download button visibility opposite to the cancel download button.
        isCancelDownloadButtonVisible.observeForever { isCancelDownloadButtonVisible ->
            isDownloadButtonVisible.value = !isCancelDownloadButtonVisible
        }
    }

    fun initOnce(
        repositoryParams: SimpleGalleryMediaRepository.Params,
        areActionsEnabled: Boolean,
    ) {
        if (isInitialized) {
            log.debug {
                "init(): already_initialized"
            }

            return
        }

        galleryMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)
            .checkNotNull {
                "The repository must be created beforehand"
            }
        subscribeToRepository()

        this.areActionsEnabled = areActionsEnabled
        initControlsVisibility()

        log.debug {
            "init(): initialized:" +
                    "\nrepositoryParam=$repositoryParams"
        }

        update()

        isInitialized = true
    }

    private fun initControlsVisibility() {
        fun updateControlsVisibility(isFullScreen: Boolean) {
            areActionsVisible.value = !isFullScreen && areActionsEnabled
            isToolbarVisible.value = !isFullScreen
        }
        isFullScreen.observeForever(::updateControlsVisibility)
    }

    private fun subscribeToRepository() {
        galleryMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { broadcastItemsFromRepository() }
            .autoDispose(this)

        galleryMediaRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(isLoading::setValue)
            .autoDispose(this)

        galleryMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                log.error(it) { "subscribeToRepository(): error_occurred" }
            }
            .autoDispose(this)
    }

    private fun broadcastItemsFromRepository() {
        itemsList.value = galleryMediaRepository
            .itemsList
            .map { galleryMedia ->
                if (galleryMedia in afterAllNotViewableMedia)
                    MediaViewerPage.unsupported(galleryMedia)
                else
                    MediaViewerPage.fromGalleryMedia(galleryMedia)
            }
    }

    private fun update() {
        galleryMediaRepository.updateIfNotFresh()
    }

    fun loadMore() {
        if (!galleryMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }
            galleryMediaRepository.loadMore()
        }
    }

    fun onShareClicked(position: Int) {
        val item = galleryMediaRepository.itemsList[position]

        log.debug {
            "onShareClicked(): start_sharing:" +
                    "\nitem=$item"
        }

        stateSubject.onNext(State.Sharing)

        if (item.files.size > 1) {
            openFileSelectionDialog(item.files)
        } else {
            downloadAndShareFile(item.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    fun onOpenInClicked(position: Int) {
        val item = galleryMediaRepository.itemsList[position]

        log.debug {
            "onOpenInClicked(): start_opening:" +
                    "\nitem=$item"
        }

        stateSubject.onNext(State.OpeningIn)

        if (item.files.size > 1) {
            openFileSelectionDialog(item.files)
        } else {
            downloadAndOpenFile(item.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    fun onOpenInWebViewerClicked(position: Int) {
        val item = galleryMediaRepository.itemsList[position]

        log.debug {
            "onOpenInWebViewerClicked(): opening_viewer:" +
                    "\nitem=$item"
        }

        // Do not pass HTTP credentials to the web viewer,
        // as it may be logger there.
        val safeWebViewUrl = item.webViewUrl.toHttpUrl()
            .withMaskedCredentials(placeholder = "")
            .toString()

        eventsSubject.onNext(Event.OpenWebViewer(url = safeWebViewUrl))
    }

    fun onDownloadClicked(position: Int) {
        check(isDownloadButtonVisible.value == true) {
            "The button can't be clicked while it is not visible"
        }

        val item = galleryMediaRepository.itemsList[position]

        log.debug {
            "onDownloadClicked(): start_downloading_to_external_storage:" +
                    "\nitem=$item"
        }

        startDownloadToExternalStorage(item)
    }

    fun onCancelDownloadClicked(position: Int) {
        check(isCancelDownloadButtonVisible.value == true) {
            "The button can't be clicked while it is not visible"
        }

        val item = galleryMediaRepository.itemsList[position]

        log.debug {
            "onCancelDownloadClicked(): canceling_download"
        }

        backgroundMediaFileDownloadManager.cancel(item.uid)
        unsubscribeFromDownloadProgress()
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

    private fun startDownloadToExternalStorage(media: GalleryMedia) {
        stateSubject.onNext(State.DownloadingToExternalStorage(media))

        if (isStoragePermissionNeeded) {
            log.debug {
                "startDownloadToExternalStorage(): must_check_storage_permission"
            }

            eventsSubject.onNext(Event.CheckStoragePermission)
        } else {
            log.debug {
                "startDownloadToExternalStorage(): no_need_to_check_storage_permission"
            }

            downloadToExternalStorage(media)
        }
    }

    private fun downloadToExternalStorage(media: GalleryMedia) {
        if (media.files.size > 1) {
            openFileSelectionDialog(media.files)
        } else {
            downloadFileToExternalStorage(media.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    fun onStoragePermissionResult(isGranted: Boolean) {
        log.debug {
            "onStoragePermissionResult(): received_result:" +
                    "\nisGranted=$isGranted"
        }

        when (val state = stateSubject.value!!) {
            is State.DownloadingToExternalStorage ->
                if (isGranted) {
                    downloadToExternalStorage(state.media)
                } else {
                    stateSubject.onNext(State.Idle)
                    eventsSubject.onNext(Event.ShowMissingStoragePermissionMessage)
                }

            else ->
                error("Can't handle storage permission in $state state")
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

        when (stateSubject.value.checkNotNull()) {
            State.Sharing ->
                downloadAndShareFile(file)

            State.OpeningIn ->
                downloadAndOpenFile(file)

            is State.DownloadingToExternalStorage ->
                downloadFileToExternalStorage(file)

            else ->
                error("Can't select files in ${stateSubject.value} state")
        }
    }

    fun onVideoPlayerFatalPlaybackError(page: VideoViewerPage) {
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
        fun onSuccess(destinationFile: File) {
            stateSubject.onNext(State.Idle)
            eventsSubject.onNext(
                Event.ShareDownloadedFile(
                    downloadedFile = destinationFile,
                    mimeType = file.mimeType,
                    displayName = File(file.name).name
                )
            )
        }

        val externalStorageDownloadDestination = getExternalStorageDownloadDestination(file)
        if (!isStoragePermissionNeeded && externalStorageDownloadDestination.exists()) {
            log.debug {
                "downloadAndShareFile(): use_existing_external_storage_download:" +
                        "\nfile=$file," +
                        "\nexternalStorageDownloadDestination=$externalStorageDownloadDestination"
            }

            onSuccess(externalStorageDownloadDestination)
            return
        }

        log.debug {
            "downloadAndShareFile(): start_downloading:" +
                    "\nfile=$file"
        }

        downloadMediaFileViewModel.downloadFile(
            file = file,
            destination = File(internalDownloadsDir, INTERNALLY_DOWNLOADED_FILE_DEFAULT_NAME),
            onSuccess = ::onSuccess
        )
    }

    private fun downloadAndOpenFile(file: GalleryMedia.File) {
        fun onSuccess(destinationFile: File) {
            stateSubject.onNext(State.Idle)
            eventsSubject.onNext(
                Event.OpenDownloadedFile(
                    downloadedFile = destinationFile,
                    mimeType = file.mimeType,
                    displayName = File(file.name).name,
                )
            )
        }

        val externalStorageDownloadDestination = getExternalStorageDownloadDestination(file)
        if (!isStoragePermissionNeeded && externalStorageDownloadDestination.exists()) {
            log.debug {
                "downloadAndOpenFile(): use_existing_external_storage_download:" +
                        "\nfile=$file," +
                        "\nexternalStorageDownloadDestination=$externalStorageDownloadDestination"
            }

            onSuccess(externalStorageDownloadDestination)
            return
        }

        log.debug {
            "downloadAndOpenFile(): start_downloading:" +
                    "\nfile=$file"
        }

        downloadMediaFileViewModel.downloadFile(
            file = file,
            destination = File(internalDownloadsDir, INTERNALLY_DOWNLOADED_FILE_DEFAULT_NAME),
            onSuccess = ::onSuccess
        )
    }

    private fun downloadFileToExternalStorage(file: GalleryMedia.File) {
        val destinationFile = getExternalStorageDownloadDestination(file)

        val progressObservable = backgroundMediaFileDownloadManager.enqueue(
            file = file,
            destination = destinationFile,
        )

        log.debug {
            "downloadFileToExternalStorage(): enqueued_download:" +
                    "\nfile=$file," +
                    "\ndestination=$destinationFile"
        }

        subscribeToMediaBackgroundDownloadStatus(progressObservable)

        stateSubject.onNext(State.Idle)
        eventsSubject.onNext(
            Event.ShowStartedDownloadMessage(
                destinationFileName = destinationFile.name,
            )
        )
    }

    fun onPageChanged(position: Int) {
        val item = galleryMediaRepository.itemsList[position]

        log.debug {
            "onPageChanged(): page_changed:" +
                    "\nitem=$item"
        }

        subscribeToMediaBackgroundDownloadStatus(
            statusObservable = backgroundMediaFileDownloadManager.getStatus(item.uid)
        )
        updateTitleAndSubtitle(item)
    }

    private var backgroundDownloadProgressDisposable: Disposable? = null
    private fun subscribeToMediaBackgroundDownloadStatus(
        statusObservable: Observable<out BackgroundMediaFileDownloadManager.Status>,
    ) {
        backgroundDownloadProgressDisposable?.dispose()
        backgroundDownloadProgressDisposable = statusObservable
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                // Default state.
                isCancelDownloadButtonVisible.value = false
                isDownloadCompletedIconVisible.value = false
            }
            .subscribeBy(
                onNext = { status ->
                    isCancelDownloadButtonVisible.value =
                        status is BackgroundMediaFileDownloadManager.Status.InProgress
                    isDownloadCompletedIconVisible.value =
                        status is BackgroundMediaFileDownloadManager.Status.Ended.Completed

                    if (status is BackgroundMediaFileDownloadManager.Status.InProgress) {
                        cancelDownloadButtonProgressPercent.value = status.percent.roundToInt()
                    }
                }
            )
            .autoDispose(this)
    }

    private fun unsubscribeFromDownloadProgress() {
        backgroundDownloadProgressDisposable?.dispose()
    }

    private fun updateTitleAndSubtitle(item: GalleryMedia) {
        title.value = item.title

        val takenAtLocal = item.takenAtLocal
        subtitle.value =
            if (takenAtLocal.isSameYearAs(currentLocalDate))
                utcDateTimeDateFormat.format(takenAtLocal).capitalized()
            else
                utcDateTimeYearDateFormat.format(takenAtLocal).capitalized()
    }

    private fun getExternalStorageDownloadDestination(file: GalleryMedia.File): File {
        val fileByExactName = File(externalDownloadsDir, File(file.name).name)

        return if (!fileByExactName.exists() || fileByExactName.canRead() && fileByExactName.canWrite())
        // Return a file with the exact name (as is) if it doesn't exist or accessible if it does.
            fileByExactName
        else
        // Otherwise return a file with a random unique name suffix.
            File(
                externalDownloadsDir,
                File(file.name)
                    .let {
                        it.nameWithoutExtension +
                                "_${System.currentTimeMillis()}" +
                                if (it.extension.isNotEmpty())
                                    ".${it.extension}"
                                else
                                    ""
                    }
            )
    }


    sealed interface Event {
        class OpenFileSelectionDialog(val files: List<GalleryMedia.File>) : Event

        class ShareDownloadedFile(
            val downloadedFile: File,
            val mimeType: String,
            val displayName: String,
        ) : Event

        class OpenDownloadedFile(
            val downloadedFile: File,
            val mimeType: String,
            val displayName: String,
        ) : Event

        object CheckStoragePermission : Event
        object ShowMissingStoragePermissionMessage : Event
        class ShowStartedDownloadMessage(val destinationFileName: String) : Event
        class OpenWebViewer(val url: String) : Event
    }

    sealed interface State {
        object Idle : State
        object Sharing : State
        object OpeningIn : State
        class DownloadingToExternalStorage(val media: GalleryMedia) : State
    }

    private companion object {
        private const val INTERNALLY_DOWNLOADED_FILE_DEFAULT_NAME = "downloaded"
    }
}
