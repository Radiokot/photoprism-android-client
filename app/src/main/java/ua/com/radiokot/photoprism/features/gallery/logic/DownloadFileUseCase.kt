package ua.com.radiokot.photoprism.features.gallery.logic

import android.content.Context
import android.media.MediaScannerConnection
import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import java.io.File

/**
 * Downloads the remote file at [url] to the give [destination].
 * In case of error or cancellation, safely deletes the [destination].
 *
 * If [mimeType] and [context] are set, notifies MediaScanner on completion.
 */
class DownloadFileUseCase(
    private val url: String,
    private val destination: File,
    private val mimeType: String?,
    private val observableDownloader: ObservableDownloader,
    private val context: Context?,
) {
    private val log = kLogger("DownloadFileUseCase")

    fun perform(): Observable<ObservableDownloader.Progress> {
        return observableDownloader
            .download(
                url = url,
                destination = destination,
            )
            .doOnDispose {
                try {
                    destination.delete()
                } catch (e: Exception) {
                    log.error(e) { "perform(): failed_to_delete_destination_on_dispose" }
                }
            }
            .doOnError {
                try {
                    destination.delete()
                } catch (e: Exception) {
                    log.error(e) { "perform(): failed_to_delete_destination_on_error" }
                }
            }
            .doOnComplete {
                if (mimeType != null && context != null && destination.exists()) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destination.path),
                        arrayOf(mimeType),
                        null,
                    )

                    log.debug {
                        "perform(): notified_media_scanner"
                    }
                }
            }
    }

    class Factory(
        private val observableDownloader: ObservableDownloader,
        private val context: Context?,
    ) {
        fun get(
            url: String,
            destination: File,
            mimeType: String?,
        ) =
            DownloadFileUseCase(
                url = url,
                destination = destination,
                mimeType = mimeType,
                observableDownloader = observableDownloader,
                context = context,
            )
    }
}
