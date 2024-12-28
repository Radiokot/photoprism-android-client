package ua.com.radiokot.photoprism.features.gallery.view.model

import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager

interface GalleryMediaDownloadActionsViewModelDelegate : GalleryMediaDownloadActionsViewModel {

    fun downloadGalleryMediaToExternalStorage(
        media: Collection<GalleryMedia>,
        onDownloadFinished: (List<SendableFile>) -> Unit = { _ -> },
    )

    fun downloadGalleryMediaToExternalStorageInBackground(
        media: GalleryMedia,
        onDownloadEnqueued: (SendableFile) -> Unit = { _ -> },
    )

    fun getGalleryMediaBackgroundDownloadStatus(
        mediaUid: String,
    ): Observable<out BackgroundMediaFileDownloadManager.Status>

    fun cancelGalleryMediaBackgroundDownload(
        mediaUid: String,
    )

    fun downloadAndOpenGalleryMedia(
        media: GalleryMedia,
        onDownloadFinished: (SendableFile) -> Unit = { _ -> },
    )

    fun downloadAndShareGalleryMedia(
        media: Collection<GalleryMedia>,
        onDownloadFinished: (List<SendableFile>) -> Unit = { _ -> },
        onShared: () -> Unit = {},
    )

    fun downloadAndReturnGalleryMedia(
        media: Collection<GalleryMedia>,
        onDownloadFinished: (List<SendableFile>) -> Unit = { _ -> },
    )
}
