package ua.com.radiokot.photoprism.features.gallery.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
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
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val downloadsDir: File,
    private val downloadFileUseCaseFactory: DownloadFileUseCase.Factory,
) : ViewModel() {
    private val log = kLogger("GalleryVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<GalleryMediaListItem>?> = MutableLiveData(null)
    private val eventsSubject: PublishSubject<Event> = PublishSubject.create()
    val events: Observable<Event> = eventsSubject
    val state: MutableLiveData<State> = MutableLiveData()

    fun initSelection(requestedMimeType: String?) {
        val filterMediaType = when {
            requestedMimeType == null ->
                null
            requestedMimeType.startsWith("image/") ->
                GalleryMedia.TypeName.IMAGE
            requestedMimeType.startsWith("video/") ->
                GalleryMedia.TypeName.VIDEO
            else ->
                null
        }

        galleryMediaRepository = galleryMediaRepositoryFactory.get(filterMediaType)
        subscribeToRepository()

        log.debug {
            "initSelection(): initialized_selection:" +
                    "\nrequestedMimeType=$requestedMimeType," +
                    "\nmatchedFilterMediaType=$filterMediaType"
        }

        state.value = State.Selecting(filter = filterMediaType)

        update()
    }

    fun initViewing() {
        galleryMediaRepository = galleryMediaRepositoryFactory.get(null)
        subscribeToRepository()

        log.debug {
            "initViewing(): initialized_viewing"
        }

        state.value = State.Viewing

        update()

        // TODO: Implement view mode
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

    private fun update() {
        galleryMediaRepository.updateIfNotFresh()
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

        when (state.value) {
            is State.Selecting -> {
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

            is State.Viewing ->
                if (item.source != null) {
                    openViewer(item.source)
                }
            else -> {
                //TODO: Implement viewing
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

    private var downloadDisposable: Disposable? = null
    private fun downloadFile(file: GalleryMedia.File) {
        val destinationFile = File(downloadsDir, "downloaded")

        log.debug {
            "downloadFile(): start_downloading:" +
                    "\nfile=$file," +
                    "\nurl=${file.downloadUrl}" +
                    "\ndestinationFile=$destinationFile"
        }

        downloadDisposable?.dispose()
        downloadDisposable = downloadFileUseCaseFactory
            .get(
                url = file.downloadUrl,
                destination = destinationFile,
            )
            .perform()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                eventsSubject.onNext(Event.ShowDownloadProgress(-1.0))
            }
            .subscribeBy(
                onNext = { progress ->
                    val percent = progress.percent

                    log.debug {
                        "downloadFile(): download_in_progress:" +
                                "\nprogress=$percent"
                    }

                    eventsSubject.onNext(Event.ShowDownloadProgress(percent))
                },
                onError = { error ->
                    log.error(error) { "downloadFile(): error_occurred" }

                    eventsSubject.onNext(Event.DismissDownloadProgress)
                    eventsSubject.onNext(Event.ShowDownloadError)
                },
                onComplete = {
                    log.debug { "downloadFile(): download_complete" }

                    eventsSubject.onNext(Event.DismissDownloadProgress)
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

    fun onDownloadProgressDialogCancelled() {
        log.debug {
            "onDownloadProgressDialogCancelled(): cancelling_download"
        }

        downloadDisposable?.dispose()
    }

    private fun openViewer(media: GalleryMedia) {
        val index = galleryMediaRepository.itemsList.indexOf(media)
        val repositoryKey = galleryMediaRepositoryFactory.keyOf(galleryMediaRepository)
            .checkNotNull {
                "The repository must have a factory key"
            }

        log.debug {
            "openViewer(): opening_viewer:" +
                    "\nmedia=$media," +
                    "\nindex=$index," +
                    "\nrepositoryKey=$repositoryKey"
        }

        eventsSubject.onNext(
            Event.OpenViewer(
                mediaIndex = index,
                repositoryKey = repositoryKey,
            )
        )
    }

    sealed interface Event {
        class OpenFileSelectionDialog(val files: List<GalleryMedia.File>) : Event
        class ShowDownloadProgress(val percent: Double) : Event
        object DismissDownloadProgress : Event
        object ShowDownloadError : Event
        class ReturnDownloadedFile(
            val downloadedFile: File,
            val mimeType: String,
            val displayName: String,
        ) : Event

        class OpenViewer(
            val mediaIndex: Int,
            val repositoryKey: String,
        ) : Event
    }

    sealed interface State {
        object Viewing : State
        class Selecting(val filter: GalleryMedia.TypeName?) : State
    }
}