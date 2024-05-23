package ua.com.radiokot.photoprism.features.gallery.view.model

import android.os.Build
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class DownloadMediaFileViewModel(
    private val downloadFileUseCaseFactory: DownloadFileUseCase.Factory,
) : ViewModel(), DownloadProgressViewModel {
    private val log = kLogger("DownloadMediaFileVM")

    private val downloadStateSubject = BehaviorSubject.create<DownloadProgressViewModel.State>()
    override val downloadState: Observable<DownloadProgressViewModel.State> =
        downloadStateSubject.toMainThreadObservable()

    private val downloadEventsSubject = PublishSubject.create<DownloadProgressViewModel.Event>()
    override val downloadEvents: Observable<DownloadProgressViewModel.Event> =
        downloadEventsSubject.toMainThreadObservable()

    private var lastDownloadedFile: DownloadedFile? = null

    val isExternalDownloadStoragePermissionRequired: Boolean
        get() = Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q)

    private var downloadDisposable: Disposable? = null
    fun downloadFile(
        file: GalleryMedia.File,
        destination: File,
        onSuccess: (destinationFile: File) -> Unit,
    ) {
        val downloadUrl = file.downloadUrl

        val alreadyDownloadedFile = this.lastDownloadedFile
        if (alreadyDownloadedFile?.url == downloadUrl
            && destination == alreadyDownloadedFile.destination
            && alreadyDownloadedFile.destination.exists()
        ) {
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
                    "\ndestination=$destination"
        }

        downloadDisposable?.dispose()
        downloadDisposable = downloadFileUseCaseFactory
            .get(
                url = file.downloadUrl,
                destination = destination,
                mimeType = file.mimeType,
            )
            .invoke()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                downloadStateSubject.onNext(DownloadProgressViewModel.State.Running())
            }
            .doOnDispose {
                try {
                    destination.delete()
                } catch (e: Exception) {
                    log.error(e) { "downloadFile(): failed_to_delete_destination_on_dispose" }
                }
            }
            .subscribeBy(
                onNext = { progress ->
                    downloadStateSubject.onNext(
                        DownloadProgressViewModel.State.Running(
                            percent = progress.percent.roundToInt(),
                        )
                    )
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
                        destination = destination,
                    )
                    downloadStateSubject.onNext(DownloadProgressViewModel.State.Idle)

                    onSuccess(destination)
                }
            )
            .autoDispose(this)
    }

    fun downloadFiles(
        filesAndDestinations: List<Pair<GalleryMedia.File, File>>,
        onSuccess: (destinationFiles: List<File>) -> Unit,
    ) {
        log.debug {
            "downloadFiles(): start_downloading:" +
                    "\nfilesCount=${filesAndDestinations.size}"
        }

        val destinations = filesAndDestinations.map(Pair<*, File>::second)

        downloadDisposable?.dispose()
        downloadDisposable = filesAndDestinations
            .mapIndexed { currentDownloadIndex, (file, destination) ->
                val downloadUrl = file.downloadUrl

                downloadFileUseCaseFactory
                    .get(
                        url = downloadUrl,
                        destination = destination,
                        mimeType = file.mimeType,
                    )
                    .invoke()
                    .subscribeOn(Schedulers.io())
                    .throttleLatest(500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        log.debug {
                            "downloadFiles(): file_download_started:" +
                                    "\nurl=$downloadUrl" +
                                    "\ncurrentDownloadIndex=$currentDownloadIndex"
                        }

                        downloadStateSubject.onNext(
                            DownloadProgressViewModel.State.Running(
                                currentDownloadNumber = currentDownloadIndex + 1,
                                downloadsCount = filesAndDestinations.size,
                            )
                        )
                    }
                    .doOnNext { progress ->
                        downloadStateSubject.onNext(
                            DownloadProgressViewModel.State.Running(
                                percent = progress.percent.roundToInt().coerceAtLeast(1),
                                currentDownloadNumber = currentDownloadIndex + 1,
                                downloadsCount = filesAndDestinations.size,
                            )
                        )
                    }
                    .ignoreElements()
            }
            .let(Completable::concat)
            .doOnDispose {
                try {
                    destinations.forEach(File::delete)
                } catch (e: Exception) {
                    log.error(e) { "downloadFiles(): failed_to_delete_destinations_on_dispose" }
                }
            }
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "downloadFiles(): error_occurred"
                    }

                    try {
                        destinations.forEach(File::delete)
                    } catch (e: Exception) {
                        log.error(e) { "downloadFiles(): failed_to_delete_destinations_on_error" }
                    }

                    downloadEventsSubject.onNext(DownloadProgressViewModel.Event.DownloadFailed)
                    downloadStateSubject.onNext(DownloadProgressViewModel.State.Idle)
                },
                onComplete = {
                    log.debug {
                        "downloadFiles(): download_complete"
                    }

                    downloadStateSubject.onNext(DownloadProgressViewModel.State.Idle)

                    onSuccess(destinations)
                }
            )
            .autoDispose(this)
    }

    override fun onDownloadProgressDialogCancelled() {
        log.debug {
            "onDownloadProgressDialogCancelled(): cancelling_download"
        }

        downloadDisposable?.dispose()
        downloadStateSubject.onNext(DownloadProgressViewModel.State.Idle)
    }

    /**
     * @return a [File] destination to download the [file] with its original name.
     * If such a file already exists and not accessible, a suffix is added to avoid overwriting.
     */
    fun getExternalDownloadDestination(
        downloadsDirectory: File,
        file: GalleryMedia.File,
    ): File {
        val fileByExactName = File(downloadsDirectory, File(file.name).name)

        return if (!fileByExactName.exists() || fileByExactName.canRead() && fileByExactName.canWrite())
        // Return a file with the exact name (as is) if it doesn't exist or accessible if it does.
            fileByExactName
        else
        // Otherwise return a file with a random unique name suffix.
            File(
                downloadsDirectory,
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

    /**
     * @return a [File] destination to download a file into the app internal storage,
     * when the name doesn't matter.
     *
     * @param index optional file index if downloading multiple files in a row
     */
    fun getInternalDownloadDestination(
        downloadsDirectory: File,
        index: Int? = null,
    ) =
        if (index == null)
            File(downloadsDirectory, "downloaded")
        else
            File(downloadsDirectory, "downloaded_$index")

    private class DownloadedFile(
        val url: String,
        val destination: File,
    )
}
