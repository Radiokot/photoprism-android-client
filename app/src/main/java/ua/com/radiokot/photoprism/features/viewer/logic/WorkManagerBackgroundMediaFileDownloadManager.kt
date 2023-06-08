package ua.com.radiokot.photoprism.features.viewer.logic

import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import java.io.File

/**
 * A [BackgroundMediaFileDownloadManager] which utilizes [WorkManager],
 * so its enqueued downloads are resilient.
 */
class WorkManagerBackgroundMediaFileDownloadManager(
    private val workManager: WorkManager,
) : BackgroundMediaFileDownloadManager {
    private val log = kLogger("WMBackgroundMFDownloadManager")

    override fun enqueue(
        file: GalleryMedia.File,
        destination: File,
    ): Observable<BackgroundMediaFileDownloadManager.Progress> {
        workManager
            .beginWith(
                OneTimeWorkRequest.Builder(DownloadFileWorker::class.java)
                    .setInputData(
                        DownloadFileWorker.getInputData(
                            url = file.downloadUrl,
                            destination = destination,
                            mimeType = file.mimeType,
                        )
                    )
                    .addTag(file.mediaUid)
                    .build()
            )
            .enqueue()

        log.debug {
            "enqueue(): enqueued:" +
                    "\nfile=$file," +
                    "\ndestination=$destination"
        }

        return getProgress(file.mediaUid)
    }

    override fun getProgress(
        mediaUid: String,
    ): Observable<BackgroundMediaFileDownloadManager.Progress> = Observable.create { emitter ->
        // Assume that we can't have simultaneous download of multiple files of the same media.
        val liveData = workManager.getWorkInfosByTagLiveData(mediaUid)

        val observer = object : Observer<List<WorkInfo>> {
            override fun onChanged(workInfoList: List<WorkInfo>) {
                if (workInfoList.isEmpty()) {
                    liveData.removeObserver(this)
                    emitter.onComplete()
                } else {
                    val workInfo = workInfoList.firstOrNull {
                        it.state in setOf(
                            State.ENQUEUED,
                            State.BLOCKED,
                            State.RUNNING
                        )
                    }

                    when (workInfo?.state) {
                        State.ENQUEUED,
                        State.BLOCKED ->
                            emitter.onNext(BackgroundMediaFileDownloadManager.Progress.INDETERMINATE)

                        State.RUNNING ->
                            emitter.onNext(
                                BackgroundMediaFileDownloadManager.Progress(
                                    percent = DownloadFileWorker.getProgressPercent(workInfo.progress)
                                )
                            )

                        else -> {
                            liveData.removeObserver(this)
                            emitter.onComplete()
                        }
                    }
                }
            }
        }

        emitter.setDisposable(object : Disposable {
            private var isDisposed = false
            override fun dispose() {
                isDisposed = true
                liveData.removeObserver(observer)
            }

            override fun isDisposed(): Boolean =
                isDisposed

        })
        liveData.observeForever(observer)
    }

}
