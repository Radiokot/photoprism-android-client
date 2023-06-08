package ua.com.radiokot.photoprism.features.viewer.logic

import android.content.Context
import android.media.MediaScannerConnection
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import java.io.File
import java.util.concurrent.Executors

/**
 * A [BackgroundMediaFileDownloadManager] which utilizes a fixed thread pool.
 *
 * @param context required to notify MediaScanner
 */
class ThreadPoolBackgroundMediaFileDownloadManager(
    private val downloadFileUseCaseFactory: DownloadFileUseCase.Factory,
    private val context: Context?,
    poolSize: Int,
) : BackgroundMediaFileDownloadManager {
    private val log = kLogger("RxBackgroundMFDownloadManager")
    private val downloads =
        mutableMapOf<String, Observable<BackgroundMediaFileDownloadManager.Progress>>()
    private val scheduler = Schedulers.from(Executors.newFixedThreadPool(poolSize))

    override fun enqueue(
        file: GalleryMedia.File,
        destination: File
    ): Observable<BackgroundMediaFileDownloadManager.Progress> {
        val key = file.mediaUid

        return Observable
            // At first, publish the indeterminate progress,
            // without waiting for a free thread from the pool.
            .just(BackgroundMediaFileDownloadManager.Progress.INDETERMINATE)
            .mergeWith(
                // Then merge it with the actual download progress,
                // which execution may be delayed.
                downloadFileUseCaseFactory
                    .get(
                        url = file.downloadUrl,
                        destination = destination,
                    )
                    .perform()
                    .subscribeOn(scheduler)
                    .map { progress ->
                        BackgroundMediaFileDownloadManager.Progress(
                            percent = progress.percent,
                        )
                    }
            )
            .doOnError {
                log.error(it) {
                    "enqueue(): download_error_occurred"
                }
            }
            .doOnComplete {
                log.debug {
                    "enqueue(): download_complete"
                }

                if (context != null && destination.exists()) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destination.path),
                        arrayOf(file.mimeType),
                        null,
                    )

                    log.debug {
                        "enqueue(): notified_media_scanner"
                    }
                }

                downloads.remove(key)
            }
            // Ignore errors to comply with the interface contract.
            .onErrorComplete()
            // Replay the last progress update for all new subscribers,
            // so the UI can set the latest state immediately.
            .replay(1)
            .also {
                downloads[key] = it

                // Start now, making the observable hot.
                it.connect()

                log.debug {
                    "enqueue(): enqueued:" +
                            "\nfile=$file," +
                            "\ndestination=$destination"
                }
            }
    }

    override fun getProgress(mediaUid: String): Observable<BackgroundMediaFileDownloadManager.Progress> {
        return downloads[mediaUid]
            ?: Observable.empty()
    }
}
