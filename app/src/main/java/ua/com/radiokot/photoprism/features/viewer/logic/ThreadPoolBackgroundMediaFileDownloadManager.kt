package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import java.io.File
import java.util.concurrent.Executors

/**
 * A [BackgroundMediaFileDownloadManager] which utilizes a fixed thread pool.
 */
class ThreadPoolBackgroundMediaFileDownloadManager(
    private val downloadFileUseCaseFactory: DownloadFileUseCase.Factory,
    poolSize: Int,
) : BackgroundMediaFileDownloadManager {
    private val log = kLogger("RxBackgroundMFDownloadManager")
    private val downloadsInProgress =
        mutableMapOf<String, Pair<Observable<BackgroundMediaFileDownloadManager.Status>, Disposable>>()
    private val endedDownloadStatuses =
        mutableMapOf<String, BackgroundMediaFileDownloadManager.Status.Ended>()
    private val scheduler = Schedulers.from(Executors.newFixedThreadPool(poolSize))

    override fun enqueue(
        file: GalleryMedia.File,
        destination: File
    ): Observable<out BackgroundMediaFileDownloadManager.Status> {
        val key = file.mediaUid

        return Observable
            // At first, publish the indeterminate progress,
            // without waiting for a free thread from the pool.
            .just<BackgroundMediaFileDownloadManager.Status>(
                BackgroundMediaFileDownloadManager.Status.InProgress.INDETERMINATE
            )
            .mergeWith(
                // Then merge it with the actual download progress,
                // which execution may be delayed.
                downloadFileUseCaseFactory
                    .get(
                        url = file.downloadUrl,
                        destination = destination,
                        mimeType = file.mimeType
                    )
                    .invoke()
                    .subscribeOn(scheduler)
                    .map { progress ->
                        BackgroundMediaFileDownloadManager.Status.InProgress(
                            percent = progress.percent,
                        )
                    }
            )
            .concatWith(Observable.just(BackgroundMediaFileDownloadManager.Status.Ended.Completed))
            .doOnError {
                log.error(it) {
                    "enqueue(): download_error_occurred:" +
                            "\nkey=$key"
                }
            }
            .doOnComplete {
                log.debug {
                    "enqueue(): download_complete:" +
                            "\nkey=$key"
                }
            }
            // Ignore errors to comply with the interface contract.
            .onErrorReturnItem(BackgroundMediaFileDownloadManager.Status.Ended.Failed)
            // Save ended status to the separate map.
            .doOnNext { status ->
                if (status is BackgroundMediaFileDownloadManager.Status.Ended) {
                    endedDownloadStatuses[key] = status

                    log.debug {
                        "enqueue(): saved_ended_status:" +
                                "\nkey=$key," +
                                "\nstatus=$status"
                    }

                    downloadsInProgress.remove(key)
                }
            }
            // Replay the last progress update for all new subscribers,
            // so the UI can set the latest state immediately.
            .replay(1)
            .also {
                // Start now, making the observable hot.
                downloadsInProgress[key] = it to it.connect()

                log.debug {
                    "enqueue(): enqueued:" +
                            "\nfile=$file," +
                            "\ndestination=$destination," +
                            "\nkey=$key"
                }
            }
    }

    override fun cancel(mediaUid: String) {
        val disposable = downloadsInProgress[mediaUid]?.second

        if (disposable == null) {
            log.debug {
                "cancel(): download_not_found:" +
                        "\nmediaUid=$mediaUid"
            }
            return
        }

        if (disposable.isDisposed) {
            log.debug {
                "cancel(): download_already_ended:" +
                        "\nmediaUid=$mediaUid"
            }
            return
        }

        disposable.dispose()

        log.debug {
            "cancel(): download_canceled:" +
                    "\nmediaUid=$mediaUid"
        }
    }

    override fun getStatus(mediaUid: String): Observable<out BackgroundMediaFileDownloadManager.Status> {
        return downloadsInProgress[mediaUid]
            ?.first
            ?: endedDownloadStatuses[mediaUid]?.let { Observable.just(it) }
            ?: Observable.empty()
    }
}
