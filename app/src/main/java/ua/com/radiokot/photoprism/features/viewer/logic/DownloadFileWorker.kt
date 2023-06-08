package ua.com.radiokot.photoprism.features.viewer.logic

import android.content.Context
import android.media.MediaScannerConnection
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import java.io.File

/**
 * A worker which downloads a remote file to a local destination
 * using [DownloadFileUseCase]
 *
 * @see getInputData
 */
class DownloadFileWorker(
    context: Context,
    workerParams: WorkerParameters,
) : RxWorker(context, workerParams), KoinScopeComponent {
    private val log = kLogger("DownloadFileWorker")
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val downloadFileUseCaseFactory: DownloadFileUseCase.Factory by inject()

    private val url = workerParams.inputData.getString(URL_KEY).checkNotNull {
        "The URL must be specified"
    }
    private val destination = File(
        workerParams.inputData.getString(DESTINATION_PATH_KEY).checkNotNull {
            "The destination path must be specified"
        }
    )
    private val mimeType =
        workerParams.inputData.getString(MIME_TYPE_KEY)?.takeIf(String::isNotEmpty)

    override fun createWork(): Single<Result> =
        downloadFileUseCaseFactory
            .get(
                url = url,
                destination = destination,
            )
            .perform()
            .doOnError {
                log.error(it) {
                    "createWork(): error_occurred"
                }
            }
            .doOnSubscribe {
                log.debug {
                    "createWork(): started:" +
                            "\nurl=$url," +
                            "\ndestination=$destination," +
                            "\nmimeType=$mimeType"
                }

                setProgressAsync(getProgressData(-1.0))
            }
            .concatMapCompletable { progress ->
                setCompletableProgress(
                    getProgressData(
                        percent = progress.percent
                    )
                )
            }
            .toSingleDefault(Result.success())
            .doOnSuccess {
                if (mimeType != null) {
                    MediaScannerConnection.scanFile(
                        applicationContext,
                        arrayOf(destination.path),
                        arrayOf(mimeType),
                        null,
                    )

                    log.debug {
                        "createWork(): notified_media_scanner"
                    }
                }

                log.debug {
                    "createWork(): successfully_done"
                }
            }
            .onErrorReturnItem(Result.failure())

    companion object {
        private const val URL_KEY = "url"
        private const val DESTINATION_PATH_KEY = "destination_path"
        private const val PROGRESS_PERCENT_KEY = "progress"
        private const val MIME_TYPE_KEY = "mime_type"

        /**
         * @param url URL of the remote file
         * @param mimeType optional, to be passed to MediaScanner
         * @param destination local destination for the download
         */
        fun getInputData(
            url: String,
            mimeType: String?,
            destination: File,
        ): Data =
            Data.Builder()
                .putString(URL_KEY, url)
                .putString(DESTINATION_PATH_KEY, destination.path)
                .putString(MIME_TYPE_KEY, mimeType)
                .build()

        private fun getProgressData(percent: Double): Data =
            Data.Builder()
                .putDouble(PROGRESS_PERCENT_KEY, percent)
                .build()

        fun getProgressPercent(data: Data): Double =
            data.getDouble(PROGRESS_PERCENT_KEY, -1.0)
    }
}
