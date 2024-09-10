package ua.com.radiokot.photoprism.features.gallery.view.model

import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager

interface MediaFileDownloadActionsViewModelDelegate : MediaFileDownloadActionsViewModel {

    fun downloadMediaFilesToExternalStorage(
        files: Collection<GalleryMedia.File>,
        onDownloadFinished: (List<SendableFile>) -> Unit = { _ -> },
    )

    fun downloadMediaFileToExternalStorageInBackground(
        file: GalleryMedia.File,
        onDownloadEnqueued: (SendableFile) -> Unit = { _ -> },
    )

    fun getMediaFileBackgroundDownloadStatus(
        mediaUid: String,
    ): Observable<out BackgroundMediaFileDownloadManager.Status>

    fun cancelMediaFileBackgroundDownload(
        mediaUid: String,
    )

    fun downloadAndOpenMediaFile(
        file: GalleryMedia.File,
        onDownloadFinished: (SendableFile) -> Unit = { _ -> },
    )

    fun downloadAndShareMediaFiles(
        files: Collection<GalleryMedia.File>,
        onDownloadFinished: (List<SendableFile>) -> Unit = { _ -> },
        onShared: () -> Unit = {},
    )

    fun downloadAndReturnMediaFiles(
        files: Collection<GalleryMedia.File>,
        onDownloadFinished: (List<SendableFile>) -> Unit = { _ -> },
    )
}
