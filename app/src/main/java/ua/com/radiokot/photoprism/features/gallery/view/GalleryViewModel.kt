package ua.com.radiokot.photoprism.features.gallery.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaListItem
import java.io.File

class GalleryViewModel(
    private val galleryMediaRepository: SimpleGalleryMediaRepository,
    private val downloadsDir: File,
    private val downloadFileUseCaseFactory: DownloadFileUseCase.Factory,
) : ViewModel() {
    private val log = kLogger("GalleryVM")

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<GalleryMediaListItem>?> = MutableLiveData(null)
    private val eventsSubject: PublishSubject<Event> = PublishSubject.create()
    val events: Observable<Event> = eventsSubject

    init {
        log.debug { "init(): initializing" }

        subscribeToRepository()

        galleryMediaRepository.updateIfNotFresh()
    }

    private fun subscribeToRepository() {
        galleryMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .map { galleryMediaItems ->
                galleryMediaItems.map(::GalleryMediaListItem)
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

    fun loadMore() {
        if (!galleryMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }
            galleryMediaRepository.loadMore()
        }
    }

    fun onItemClicked(item: GalleryMediaListItem) {
        log.debug {
            "onItemClicked(): gallery_item_clicked:" +
                    "\nitem=$item"
        }

        // TODO: Different behavior for different modes.

        if (item.source != null) {
            if (item.source.files.size > 1) {
                openFileSelectionDialog(item.source.files)
            } else {
                downloadFile(item.source.files.firstOrNull().checkNotNull {
                    "There must be at least one file in the gallery media object"
                })
            }
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

        downloadFile(file)
    }

    private fun downloadFile(file: GalleryMedia.File) {
        val destinationFile = File(downloadsDir, "downloaded")

        log.debug {
            "downloadFile(): start_downloading:" +
                    "\nfile=$file," +
                    "\ndestinationFile=$destinationFile"
        };

        downloadFileUseCaseFactory
            .get(
                // TODO: Replace with the file download URL.
                url = file.thumbnailUrlSmall,
                destination = destinationFile,
            )
            .perform()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { progress ->
                    log.debug {
                        "downloadFile(): download_in_progress:" +
                                "\nprogress=${progress.percent}"
                    }
                    // TODO: Report
                },
                onError = { error ->
                    log.error(error) { "downloadFile(): error_occurred" }
                    // TODO: Report
                },
                onComplete = {
                    log.debug { "downloadFile(): download_complete" }

                    eventsSubject.onNext(
                        Event.ReturnDownloadedFile(
                            downloadedFile = destinationFile,
                            mimeType = file.mimeType,
                            displayName = File(file.name).name
                        )
                    )
                }
            )
            .addToCloseables(this)
    }

    sealed interface Event {
        class OpenFileSelectionDialog(val files: List<GalleryMedia.File>) : Event
        class ReturnDownloadedFile(
            val downloadedFile: File,
            val mimeType: String,
            val displayName: String,
        ) : Event
    }
}