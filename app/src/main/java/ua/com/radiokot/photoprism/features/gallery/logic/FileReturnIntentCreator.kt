package ua.com.radiokot.photoprism.features.gallery.logic

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore.MediaColumns
import androidx.core.content.FileProvider
import ua.com.radiokot.photoprism.extension.kLogger
import java.io.File

/**
 * @param [fileProviderAuthority] authority of the updatable [FileProvider]
 *
 * @see UpdatableFileProvider
 */
class FileReturnIntentCreator(
    private val fileProviderAuthority: String,
    private val context: Context,
) {
    private val log = kLogger("FileReturnIntentCreator")

    /**
     * Creates a result [Intent] returning [fileToReturn]
     * with the given [mimeType] and [displayName],
     * assuming that the [FileProvider] of [fileProviderAuthority]
     * supports MIME-type updates.
     */
    fun createIntent(
        fileToReturn: File,
        mimeType: String,
        displayName: String,
    ): Intent {
        val uri = FileProvider.getUriForFile(
            context, fileProviderAuthority,
            fileToReturn, displayName
        )

        log.debug {
            "createIntent(): got_uri:" +
                    "\nuri=$uri," +
                    "\nfileToReturn=$fileToReturn," +
                    "\ndisplayName=$displayName"
        }

        val updateContentValues = ContentValues().apply {
            put(MediaColumns.MIME_TYPE, mimeType)
        }
        val updateMimeTypeResult =
            context.contentResolver.update(uri, updateContentValues, null, null)

        check(updateMimeTypeResult > 0) {
            "MIME-type update must be successful"
        }

        log.debug {
            "createIntent(): updated_mime_type:" +
                    "\nuri=$uri," +
                    "\nmimeType=$mimeType"
        }

        return Intent().apply {
            setDataAndType(uri, mimeType)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}