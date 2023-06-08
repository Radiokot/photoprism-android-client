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

class BackgroundMediaFileDownloadManager(
    private val workManager: WorkManager,
) {
    private val log = kLogger("BackgroundMFDownloadManager")

    /**
     * Enqueues a background download task which will be executed ASAP.
     *
     * @return hot progress observable. It completes if the download is ended and doesn't emit errors.
     *
     * @see getProgress
     */
    fun enqueue(
        file: GalleryMedia.File,
        destination: File,
    ): Observable<Progress> {
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

    /**
     * @return hot progress observable for the file download of the media with the given UID.
     * It completes if the download is ended or not found, it doesn't emit errors.
     */
    fun getProgress(
        mediaUid: String,
    ): Observable<Progress> = Observable.create { emitter ->
        // Assume that we can't have simultaneous download of multiple files of the same media.
        val liveData = workManager.getWorkInfosByTagLiveData(mediaUid)

        val observer = object : Observer<List<WorkInfo>> {
            override fun onChanged(workInfoList: List<WorkInfo>) {
                if (workInfoList.isEmpty()) {
                    liveData.removeObserver(this)
                    emitter.onComplete()
                } else {
                    // TODO: bug when there are lot of previously completed works.
                    val workInfo = workInfoList.last()
                    when (workInfo.state) {
                        State.ENQUEUED,
                        State.BLOCKED ->
                            emitter.onNext(Progress.INDETERMINATE)

                        State.RUNNING ->
                            emitter.onNext(
                                Progress(
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

    class Progress(
        val percent: Double,
    ) {
        companion object {
            val INDETERMINATE = Progress(percent = -1.0)
        }
    }
}
