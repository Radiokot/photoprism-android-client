package ua.com.radiokot.photoprism.features.gallery.view

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import java.io.File

class DownloadMediaFileViewModel(
    private val downloadsDir: File,
    private val downloadFileUseCaseFactory: DownloadFileUseCase.Factory,
) : ViewModel(), DownloadProgressViewModel {
    private val log = kLogger("DownloadMediaFileVM")

    private val downloadStateSubject = BehaviorSubject.create<DownloadProgressViewModel.State>()
    override val downloadState: Observable<DownloadProgressViewModel.State> = downloadStateSubject

    private val downloadEventsSubject = PublishSubject.create<DownloadProgressViewModel.Event>()
    override val downloadEvents: Observable<DownloadProgressViewModel.Event> = downloadEventsSubject

    private var lastDownloadedFile: DownloadedFile? = null

    private var downloadDisposable: Disposable? = null
    fun downloadFile(
        file: GalleryMedia.File,
        onSuccess: (File) -> Unit,
    ) {
        val destinationFile = File(downloadsDir, "downloaded")
        val downloadUrl = file.downloadUrl

        val alreadyDownloadedFile = this.lastDownloadedFile
        if (alreadyDownloadedFile?.url == downloadUrl && alreadyDownloadedFile.destination.exists()) {
            log.debug {
                "downloadFile(): return_already_downloaded_file:" +
                        "\nurl=$downloadUrl" +
                        "\ndestinationFile=${alreadyDownloadedFile.destination}"
            }

            onSuccess(alreadyDownloadedFile.destination)
            return
        }

        log.debug {
            "downloadFile(): start_downloading:" +
                    "\nfile=$file," +
                    "\nurl=$downloadUrl" +
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
                downloadStateSubject.onNext(DownloadProgressViewModel.State.Running(-1.0))
            }
            .subscribeBy(
                onNext = { progress ->
                    val percent = progress.percent

                    log.debug {
                        "downloadFile(): download_in_progress:" +
                                "\nurl=$downloadUrl" +
                                "\nprogress=$percent"
                    }

                    downloadStateSubject.onNext(DownloadProgressViewModel.State.Running(progress.percent))
                },
                onError = { error ->
                    log.error(error) {
                        "downloadFile(): error_occurred:" +
                                "\nurl=$downloadUrl"
                    }

                    downloadEventsSubject.onNext(DownloadProgressViewModel.Event.DownloadFailed)
                    downloadStateSubject.onNext(DownloadProgressViewModel.State.Idle)
                },
                onComplete = {
                    log.debug {
                        "downloadFile(): download_complete:" +
                                "\nurl=$downloadUrl"
                    }

                    this.lastDownloadedFile = DownloadedFile(
                        url = downloadUrl,
                        destination = destinationFile,
                    )
                    downloadStateSubject.onNext(DownloadProgressViewModel.State.Idle)

                    onSuccess(destinationFile)
                }
            )
            .addToCloseables(this)
    }

    override fun onDownloadProgressDialogCancelled() {
        log.debug {
            "onDownloadProgressDialogCancelled(): cancelling_download"
        }

        downloadDisposable?.dispose()
        downloadStateSubject.onNext(DownloadProgressViewModel.State.Idle)
    }

    private class DownloadedFile(
        val url: String,
        val destination: File,
    )
}