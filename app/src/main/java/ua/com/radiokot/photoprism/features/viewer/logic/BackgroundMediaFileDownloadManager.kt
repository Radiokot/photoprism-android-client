package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import java.io.File

interface BackgroundMediaFileDownloadManager {
    /**
     * Enqueues a background download task which will be executed ASAP.
     *
     * @return hot status observable.
     *
     * @see getStatus
     */
    fun enqueue(
        file: GalleryMedia.File,
        destination: File,
        notifyMediaScanner: Boolean,
    ): Observable<out Status>

    /**
     * @return hot status observable for the file download of the media with the given UID.
     * For a new subscriber, it replays the last status
     * or simply completes if there is no such download.
     *
     * It does not emit errors on purpose.
     */
    fun getStatus(
        mediaUid: String,
    ): Observable<out Status>

    /**
     * Cancels the active download by [mediaUid], if there is one.
     */
    fun cancel(
        mediaUid: String,
    )

    sealed interface Status {
        class InProgress(
            val percent: Double,
        ): Status {
            companion object {
                val INDETERMINATE = InProgress(percent = -1.0)
            }
        }

        sealed interface Ended: Status {
            object Completed : Ended
            object Failed : Ended
        }
    }
}
