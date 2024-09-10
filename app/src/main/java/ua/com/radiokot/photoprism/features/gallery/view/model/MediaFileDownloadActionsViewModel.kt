package ua.com.radiokot.photoprism.features.gallery.view.model

import android.content.Intent
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator

interface MediaFileDownloadActionsViewModel : DownloadProgressViewModel {

    val mediaFileDownloadActionsEvents: PublishSubject<Event>

    fun onStoragePermissionResult(isGranted: Boolean)
    fun onDownloadedMediaFilesShared()

    sealed interface Event {
        /**
         * Return the files to the requesting app when the selection is done.
         *
         * @see FileReturnIntentCreator
         */
        class ReturnDownloadedFiles(
            val files: List<SendableFile>,
        ) : Event

        /**
         * Share the files with any app of the user's choice when the selection is done.
         *
         * Once shared, the [onDownloadedMediaFilesShared] method should be called.
         *
         * @see FileReturnIntentCreator
         */
        class ShareDownloadedFiles(
            val files: List<SendableFile>,
        ) : Event

        /**
         * Open a single file with [Intent.ACTION_VIEW].
         */
        class OpenDownloadedFile(
            val file: SendableFile
        ) : Event

        /**
         * Show a message that the files have been saved to the Downloads.
         */
        object ShowFilesDownloadedMessage : Event

        /**
         * Request the external storage write permission reporting the result
         * to the [onStoragePermissionResult] method.
         */
        object RequestStoragePermission : Event

        /**
         * Show a message explaining that the storage permission
         * is essential for the attempted action.
         */
        object ShowMissingStoragePermissionMessage : Event
    }
}
