package ua.com.radiokot.photoprism.features.viewer.view.model

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.viewer.logic.WrappedMediaScannerConnection
import java.io.File

class MediaViewerViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val internalDownloadsDir: File,
    private val externalDownloadsDir: File,
    private val mediaScannerConnection: WrappedMediaScannerConnection?,
) : ViewModel() {
    private val log = kLogger("MediaViewerVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository
    private var isInitialized = false

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPagerItem>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOn(AndroidSchedulers.mainThread())
    val state: MutableLiveData<State> = MutableLiveData(State.Idle)
    val areActionsVisible: MutableLiveData<Boolean> = MutableLiveData(true)
    val isFullScreen: MutableLiveData<Boolean> = MutableLiveData(false).apply {
        observeForever { areActionsVisible.value = !it }
    }

    private lateinit var downloadMediaFileViewModel: DownloadMediaFileViewModel

    fun initOnce(
        downloadViewModel: DownloadMediaFileViewModel,
        repositoryQuery: String?,
    ) {
        if (isInitialized) {
            log.debug {
                "init(): already_initialized"
            }

            return
        }

        downloadMediaFileViewModel = downloadViewModel

        galleryMediaRepository = galleryMediaRepositoryFactory.get(repositoryQuery)
            .checkNotNull {
                "The repository must be created beforehand"
            }
        subscribeToRepository()

        log.debug {
            "init(): initialized:" +
                    "\nrepositoryQuery=$repositoryQuery"
        }

        update()
    }

    private fun subscribeToRepository() {
        galleryMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .map { galleryMediaItems ->
                galleryMediaItems.map(MediaViewerPagerItem.Companion::fromGalleryMedia)
            }
            .subscribe(itemsList::setValue)
            .addToCloseables(this)

        galleryMediaRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(isLoading::setValue)
            .addToCloseables(this)

        galleryMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                log.error(it) { "subscribeToRepository(): error_occurred" }
            }
            .addToCloseables(this)
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

        state.value = State.Sharing

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

        state.value = State.OpeningIn

        if (item.files.size > 1) {
            openFileSelectionDialog(item.files)
        } else {
            downloadAndOpenFile(item.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    fun onDownloadClicked(position: Int) {
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
        state.value = State.DownloadingToExternalStorage(media)

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

        when (val state = state.value!!) {
            is State.DownloadingToExternalStorage ->
                if (isGranted) {
                    downloadToExternalStorage(state.media)
                } else {
                    this.state.value = State.Idle
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

        when (state.value.checkNotNull()) {
            State.Sharing ->
                downloadAndShareFile(file)
            State.OpeningIn ->
                downloadAndOpenFile(file)
            is State.DownloadingToExternalStorage ->
                downloadFileToExternalStorage(file)
            else ->
                error("Can't select files in ${state.value} state")
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
                state.value = State.Idle
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
                state.value = State.Idle
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
        log.debug {
            "downloadFileToExternalStorage(): start_downloading:" +
                    "\nfile=$file"
        }

        downloadMediaFileViewModel.downloadFile(
            file = file,
            destination = File(externalDownloadsDir, File(file.name).name),
            onSuccess = { destinationFile ->
                state.value = State.Idle
                eventsSubject.onNext(
                    Event.ShowSuccessfulDownloadMessage(
                        fileName = destinationFile.name,
                    )
                )

                mediaScannerConnection?.scanFile(
                    destinationFile.path,
                    file.mimeType,
                )?.also {
                    log.debug { "downloadFileToExternalStorage(): notified_media_scanner" }
                }
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
        class ShowSuccessfulDownloadMessage(val fileName: String) : Event
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