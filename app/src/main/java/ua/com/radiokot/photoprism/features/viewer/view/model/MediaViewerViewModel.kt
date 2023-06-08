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
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager
import java.io.File
import kotlin.math.roundToInt

class MediaViewerViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val internalDownloadsDir: File,
    private val externalDownloadsDir: File,
    val downloadMediaFileViewModel: DownloadMediaFileViewModel,
    private val backgroundMediaFileDownloadManager: BackgroundMediaFileDownloadManager,
) : ViewModel() {
    private val log = kLogger("MediaViewerVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository
    private var isInitialized = false
    private var areActionsEnabled = false

    // Media that turned to be not viewable.
    private val afterAllNotViewableMedia = mutableSetOf<GalleryMedia>()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPage>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.toMainThreadObservable()
    private val stateSubject = BehaviorSubject.createDefault<State>(State.Idle)
    val state: Observable<State> = stateSubject.toMainThreadObservable()
    val areActionsVisible: MutableLiveData<Boolean> = MutableLiveData(true)
    val isFullScreen: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadButtonProgressVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadButtonClickable: MutableLiveData<Boolean> = MutableLiveData(false)

    // From 0 to 100, negative for indeterminate.
    val downloadButtonProgressPercent: MutableLiveData<Int> = MutableLiveData(-1)

    init {
        isDownloadButtonProgressVisible.observeForever { isProgressVisible ->
            isDownloadButtonClickable.value = !isProgressVisible
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
        initActionsVisibility()

        log.debug {
            "init(): initialized:" +
                    "\nrepositoryParam=$repositoryParams"
        }

        update()
    }

    private fun initActionsVisibility() {
        fun updateActionsVisibility() {
            areActionsVisible.value =
                isFullScreen.value == false && areActionsEnabled
        }
        isFullScreen.observeForever { updateActionsVisibility() }
        updateActionsVisibility()
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

        eventsSubject.onNext(Event.OpenUrl(url = item.webViewUrl))
    }

    fun onDownloadClicked(position: Int) {
        check(isDownloadButtonClickable.value == true) {
            "The button can't be clicked while it is unclickable"
        }

        val item = galleryMediaRepository.itemsList[position]

        log.debug {
            "onDownloadClicked(): start_downloading_to_external_storage:" +
                    "\nitem=$item"
        }

        startDownloadToExternalStorage(item)
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

        if (Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q)) {
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
        log.debug {
            "downloadAndShareFile(): start_downloading:" +
                    "\nfile=$file"
        }

        downloadMediaFileViewModel.downloadFile(
            file = file,
            destination = File(internalDownloadsDir, INTERNALLY_DOWNLOADED_FILE_DEFAULT_NAME),
            onSuccess = { destinationFile ->
                stateSubject.onNext(State.Idle)
                eventsSubject.onNext(
                    Event.ShareDownloadedFile(
                        downloadedFile = destinationFile,
                        mimeType = file.mimeType,
                        displayName = File(file.name).name
                    )
                )
            }
        )
    }

    private fun downloadAndOpenFile(file: GalleryMedia.File) {
        log.debug {
            "downloadAndOpenFile(): start_downloading:" +
                    "\nfile=$file"
        }

        downloadMediaFileViewModel.downloadFile(
            file = file,
            destination = File(internalDownloadsDir, INTERNALLY_DOWNLOADED_FILE_DEFAULT_NAME),
            onSuccess = { destinationFile ->
                stateSubject.onNext(State.Idle)
                eventsSubject.onNext(
                    Event.OpenDownloadedFile(
                        downloadedFile = destinationFile,
                        mimeType = file.mimeType,
                        displayName = File(file.name).name,
                    )
                )
            }
        )
    }

    private fun downloadFileToExternalStorage(file: GalleryMedia.File) {
        val destinationFile = File(externalDownloadsDir, File(file.name).name)

        val progressObservable = backgroundMediaFileDownloadManager.enqueue(
            file = file,
            destination = destinationFile,
        )

        log.debug {
            "downloadFileToExternalStorage(): enqueued_download:" +
                    "\nfile=$file," +
                    "\ndestination=$destinationFile"
        }

        subscribeToMediaBackgroundDownloadProgress(progressObservable)

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

        subscribeToMediaBackgroundDownloadProgress(
            progressObservable = backgroundMediaFileDownloadManager.getProgress(item.uid)
        )
    }

    private var backgroundDownloadProgressDisposable: Disposable? = null
    private fun subscribeToMediaBackgroundDownloadProgress(
        progressObservable: Observable<BackgroundMediaFileDownloadManager.Progress>,
    ) {
        backgroundDownloadProgressDisposable?.dispose()
        backgroundDownloadProgressDisposable = progressObservable
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isDownloadButtonProgressVisible.value = false
            }
            .subscribeBy(
                onNext = { progress ->
                    isDownloadButtonProgressVisible.value = true
                    downloadButtonProgressPercent.value = progress.percent.roundToInt()
                },
                onComplete = {
                    isDownloadButtonProgressVisible.value = false
                },
                onError = {
                    log.error(it) {
                        "subscribeToMediaBackgroundDownloadProgress(): error_occurred"
                    }
                }
            )
            .autoDispose(this)
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
        class OpenUrl(val url: String) : Event
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
