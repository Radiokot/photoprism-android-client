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
import ua.com.radiokot.photoprism.features.gallery.view.DownloadProgressViewModel
import ua.com.radiokot.photoprism.features.gallery.view.GalleryViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPageItem
import java.io.File

class MediaViewerViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val downloadMediaFileViewModel: DownloadMediaFileViewModel,
) : ViewModel(), DownloadProgressViewModel by downloadMediaFileViewModel {
    private val log = kLogger("MediaViewerVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPageItem>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOn(AndroidSchedulers.mainThread())

    fun init(repositoryKey: String) {
        galleryMediaRepository = galleryMediaRepositoryFactory.get(repositoryKey)
            .checkNotNull {
                "The repository must be created beforehand"
            }

        subscribeToRepository()

        log.debug {
            "init(): initialized:" +
                    "\nrepositoryKey=$repositoryKey"
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

        if (item.files.size > 1) {
            // TODO: File selection dialog.
        } else {
            downloadAndShareFile(item.files.firstOrNull().checkNotNull {
                "There must be at least one file in the gallery media object"
            })
        }
    }

    private fun downloadAndShareFile(file: GalleryMedia.File) {
        downloadMediaFileViewModel.downloadFile(
            file = file,
            onSuccess = { destinationFile ->
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

    sealed interface Event {
        class ShareDownloadedFile(
            val downloadedFile: File,
            val mimeType: String,
            val displayName: String,
        ) : Event
    }
}