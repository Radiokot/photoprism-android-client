package ua.com.radiokot.photoprism.features.viewer.view

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
import ua.com.radiokot.photoprism.features.gallery.view.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPageItem
import java.io.File

class MediaViewerViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
) : ViewModel() {
    private val log = kLogger("MediaViewerVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPageItem>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOn(AndroidSchedulers.mainThread())
    val state: MutableLiveData<State> = MutableLiveData(State.Idle)

    private lateinit var downloadMediaFileViewModel: DownloadMediaFileViewModel

    fun init(
        downloadViewModel: DownloadMediaFileViewModel,
        repositoryQuery: String?,
    ) {
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
                galleryMediaItems.map(MediaViewerPageItem.Companion::fromGalleryMedia)
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
            else ->
                throw IllegalStateException("Can't select files in ${state.value} state")
        }
    }

    private fun downloadAndShareFile(file: GalleryMedia.File) {
        log.debug {
            "downloadAndShareFile(): start_downloading:" +
                    "\nfile=$file"
        }

        downloadMediaFileViewModel.downloadFile(
            file = file,
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
    }

    sealed interface State {
        object Idle : State
        object Sharing : State
        object OpeningIn : State
    }
}