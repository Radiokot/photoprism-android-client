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
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileDownloadActionsViewModel.Event
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MediaFileDownloadActionsViewModelDelegateImpl(
    private val internalDownloadsDir: File,
    private val externalDownloadsDir: File,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val backgroundMediaFileDownloadManager: BackgroundMediaFileDownloadManager,
    private val downloadUrlFactory: MediaFileDownloadUrlFactory,
) : ViewModel(),
    MediaFileDownloadActionsViewModelDelegate {

    private val log = kLogger("MediaFileDownloadActionsVMDI")

    override val mediaFileDownloadActionsEvents: PublishSubject<Event> =
        PublishSubject.create()
    override val downloadProgressState: BehaviorSubject<DownloadProgressViewModel.State> =
        BehaviorSubject.create()
    override val downloadProgressEvents: PublishSubject<DownloadProgressViewModel.Event> =
        PublishSubject.create()

    private var doOnStoragePermissionGranted: () -> Unit = {}
    private var doOnFilesShared: () -> Unit = {}
    private var downloadDisposable: Disposable? = null
    private val isExternalDownloadStoragePermissionRequired: Boolean
        get() = Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q)

    override fun downloadMediaFilesToExternalStorage(
        files: Collection<GalleryMedia.File>,
        onDownloadFinished: (List<SendableFile>) -> Unit,
    ) {
        fun doDownload() {
            downloadFiles(
                filesAndDestinations = files.map { mediaFile ->
                    mediaFile to getExternalDownloadDestination(mediaFile)
                },
                onSuccess = { sendableFiles ->
                    mediaFileDownloadActionsEvents.onNext(Event.ShowFilesDownloadedMessage)
                    onDownloadFinished(sendableFiles)
                }
            )
        }

        if (isExternalDownloadStoragePermissionRequired) {
            log.debug {
                "downloadGalleryMediaFilesToExternalStorage(): must_request_storage_permission"
            }

            doOnStoragePermissionGranted = ::doDownload

            mediaFileDownloadActionsEvents.onNext(
                Event.RequestStoragePermission
            )
        } else {
            log.debug {
                "downloadGalleryMediaFilesToExternalStorage(): no_need_to_check_storage_permission"
            }

            doDownload()
        }
    }

    override fun downloadMediaFileToExternalStorageInBackground(
        file: GalleryMedia.File,
        onDownloadEnqueued: (SendableFile) -> Unit
    ) {
        fun doEnqueue() {
            val destination = getExternalDownloadDestination(file)

            backgroundMediaFileDownloadManager.enqueue(
                file = file,
                destination = destination,
            )

            onDownloadEnqueued(SendableFile(file to destination))
        }

        if (isExternalDownloadStoragePermissionRequired) {
            log.debug {
                "downloadGalleryMediaFileToExternalStorageInBackground(): must_request_storage_permission"
            }

            doOnStoragePermissionGranted = ::doEnqueue

            mediaFileDownloadActionsEvents.onNext(
                Event.RequestStoragePermission
            )
        } else {
            log.debug {
                "downloadGalleryMediaFileToExternalStorageInBackground(): no_need_to_check_storage_permission"
            }

            doEnqueue()
        }
    }

    override fun getMediaFileBackgroundDownloadStatus(
        mediaUid: String,
    ): Observable<out BackgroundMediaFileDownloadManager.Status> =
        backgroundMediaFileDownloadManager.getStatus(mediaUid)

    override fun cancelMediaFileBackgroundDownload(mediaUid: String) =
        backgroundMediaFileDownloadManager.cancel(mediaUid)

    override fun downloadAndOpenMediaFile(
        file: GalleryMedia.File,
        onDownloadFinished: (SendableFile) -> Unit,
    ) {
        downloadFiles(
            filesAndDestinations = listOf(
                file to getInternalDownloadDestination(file)
            ),
            onSuccess = { sendableFiles ->
                mediaFileDownloadActionsEvents.onNext(
                    Event.OpenDownloadedFile(sendableFiles.first())
                )
                onDownloadFinished(sendableFiles.first())
            }
        )
    }

    override fun downloadAndShareMediaFiles(
        files: Collection<GalleryMedia.File>,
        onDownloadFinished: (List<SendableFile>) -> Unit,
        onShared: () -> Unit,
    ) {
        doOnFilesShared = onShared

        downloadFiles(
            filesAndDestinations = files.map { mediaFile ->
                mediaFile to getInternalDownloadDestination(mediaFile)
            },
            onSuccess = { sendableFiles ->
                mediaFileDownloadActionsEvents.onNext(
                    Event.ShareDownloadedFiles(sendableFiles)
                )
                onDownloadFinished(sendableFiles)
            }
        )
    }

    override fun downloadAndReturnMediaFiles(
        files: Collection<GalleryMedia.File>,
        onDownloadFinished: (List<SendableFile>) -> Unit,
    ) {
        downloadFiles(
            filesAndDestinations = files.map { mediaFile ->
                mediaFile to getInternalDownloadDestination(mediaFile)
            },
            onSuccess = { sendableFiles ->
                mediaFileDownloadActionsEvents.onNext(
                    Event.ReturnDownloadedFiles(
                        sendableFiles
                    )
                )
                onDownloadFinished(sendableFiles)
            }
        )
    }

    override fun onStoragePermissionResult(isGranted: Boolean) {
        log.debug {
            "onStoragePermissionResult(): received_result:" +
                    "\nisGranted=$isGranted"
        }

        if (isGranted) {
            doOnStoragePermissionGranted()
        } else {
            mediaFileDownloadActionsEvents.onNext(
                Event.ShowMissingStoragePermissionMessage
            )
        }
    }

    override fun onDownloadedMediaFilesShared() =
        doOnFilesShared()

    override fun onUserCancelledDownload() {
        log.debug {
            "onDownloadProgressDialogCancelled(): cancelling_download"
        }

        downloadDisposable?.dispose()
        downloadProgressState.onNext(DownloadProgressViewModel.State.Idle)
    }

    private fun downloadFiles(
        filesAndDestinations: List<Pair<GalleryMedia.File, File>>,
        onSuccess: (sendableFiles: List<SendableFile>) -> Unit,
    ) {
        log.debug {
            "downloadFiles(): start_downloading:" +
                    "\nfilesCount=${filesAndDestinations.size}"
        }

        val destinations = filesAndDestinations.map(Pair<*, File>::second)

        downloadDisposable?.dispose()
        downloadDisposable = filesAndDestinations
            .mapIndexed { currentDownloadIndex, (file, destination) ->
                val downloadUrl =  downloadUrlFactory.getDownloadUrl(
                    hash = file.hash,
                )

                if (destination.exists() && destination.canRead()) {
                    return@mapIndexed Completable.complete().doOnSubscribe {
                        log.debug {
                            "downloadFiles(): skip_already_download_file:" +
                                    "\nurl=$downloadUrl" +
                                    "\ncurrentDownloadIndex=$currentDownloadIndex"
                        }
                    }
                }

                downloadFileUseCase
                    .invoke(
                        url = downloadUrl,
                        destination = destination,
                        mimeType = file.mimeType,
                    )
                    .subscribeOn(Schedulers.io())
                    .throttleLatest(500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        log.debug {
                            "downloadFiles(): file_download_started:" +
                                    "\nurl=$downloadUrl" +
                                    "\ncurrentDownloadIndex=$currentDownloadIndex"
                        }

                        downloadProgressState.onNext(
                            DownloadProgressViewModel.State.Running(
                                currentDownloadNumber = currentDownloadIndex + 1,
                                downloadsCount = filesAndDestinations.size,
                            )
                        )
                    }
                    .doOnNext { progress ->
                        downloadProgressState.onNext(
                            DownloadProgressViewModel.State.Running(
                                percent =
                                if (progress.percent < 0)
                                    -1
                                else
                                    progress.percent.roundToInt().coerceAtLeast(1),
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

                    downloadProgressEvents.onNext(DownloadProgressViewModel.Event.DownloadFailed)
                    downloadProgressState.onNext(DownloadProgressViewModel.State.Idle)
                },
                onComplete = {
                    log.debug {
                        "downloadFiles(): download_complete"
                    }

                    downloadProgressState.onNext(DownloadProgressViewModel.State.Idle)

                    onSuccess(filesAndDestinations.map(::SendableFile))
                }
            )
            .autoDispose(this)
    }

    /**
     * @return a [File] destination to download the [file] with its original name.
     * If such a file already exists and not accessible, a suffix is added to avoid overwriting.
     */
    private fun getExternalDownloadDestination(
        file: GalleryMedia.File,
    ): File {
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

    /**
     * @return a [File] destination to download a file into the app internal storage,
     * when the name doesn't matter.
     */
    private fun getInternalDownloadDestination(
        file: GalleryMedia.File,
    ) =
        File(internalDownloadsDir, "download_${file.uid}")
}
