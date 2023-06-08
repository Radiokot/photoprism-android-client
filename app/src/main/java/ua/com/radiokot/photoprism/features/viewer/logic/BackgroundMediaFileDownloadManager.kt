package ua.com.radiokot.photoprism.features.viewer.logic

import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import java.io.File

interface BackgroundMediaFileDownloadManager {
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
    ): Observable<Progress>

    /**
     * @return hot progress observable for the file download of the media with the given UID.
     * It completes if the download is ended or not found, it doesn't emit errors.
     */
    fun getProgress(
        mediaUid: String,
    ): Observable<BackgroundMediaFileDownloadManager.Progress>

    class Progress(
        val percent: Double,
    ) {
        companion object {
            val INDETERMINATE = Progress(percent = -1.0)
        }
    }
}
