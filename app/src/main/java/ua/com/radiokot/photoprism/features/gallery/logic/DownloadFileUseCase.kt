package ua.com.radiokot.photoprism.features.gallery.logic

import android.content.Context
import android.media.MediaScannerConnection
import androidx.exifinterface.media.ExifInterface
import com.google.android.exoplayer2.util.MimeTypes
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

    operator fun invoke(): Observable<ObservableDownloader.Progress> {
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
                // Notify MediaScanner for the content to immediately appear in galleries.
                if (mimeType != null && context != null && destination.exists()) {
                    if (mimeType == MimeTypes.IMAGE_JPEG) {
                        // MediaScanner uses buggy ExifInterface implementation
                        // to determine JPEGs orientation, which fails.
                        // But! If you create (just create!) an androidx ExifInterface instance
                        // for your file which reads all the attributes correctly,
                        // it somehow gets cached and helps MediaScanner 🤡
                        ExifInterface(destination.path)

                        log.debug {
                            "perform(): created_fixed_exif_interface"
                        }
                    }

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
