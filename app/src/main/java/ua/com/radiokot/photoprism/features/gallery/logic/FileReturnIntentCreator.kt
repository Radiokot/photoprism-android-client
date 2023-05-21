package ua.com.radiokot.photoprism.features.gallery.logic

import android.content.ClipData
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
     * Creates a compatible result [Intent] returning [fileToReturn]
     * with the given [mimeType] and [displayName],
     * assuming that the [FileProvider] of [fileProviderAuthority]
     * supports MIME-type updates.
     */
    fun createIntent(
        fileToReturn: File,
        mimeType: String,
        displayName: String,
    ) = createIntent(
        listOf(
            FileToReturn(
                file = fileToReturn,
                mimeType = mimeType,
                displayName = displayName,
            )
        )
    )

    /**
     * Creates a compatible result [Intent] returning [filesToReturn]
     * with the corresponding [FileToReturn.mimeType] and [FileToReturn.displayName],
     * assuming that the [FileProvider] of [fileProviderAuthority]
     * supports MIME-type updates.
     */
    fun createIntent(filesToReturn: List<FileToReturn>): Intent {
        require(filesToReturn.isNotEmpty()) {
            "There must be at least one file to return"
        }

        val contentResolver = context.contentResolver

        val clipDataItems = filesToReturn.map { fileToReturn ->
            val uri = FileProvider.getUriForFile(
                context, fileProviderAuthority,
                fileToReturn.file, fileToReturn.displayName
            )

            log.debug {
                "createIntent(): got_uri:" +
                        "\nuri=$uri," +
                        "\nfileToReturn=$fileToReturn"
            }

            val updateContentValues = ContentValues().apply {
                put(MediaColumns.MIME_TYPE, fileToReturn.mimeType)
            }
            val updateMimeTypeResult =
                contentResolver.update(uri, updateContentValues, null, null)

            check(updateMimeTypeResult > 0) {
                "MIME-type update must be successful"
            }

            log.debug {
                "createIntent(): updated_mime_type:" +
                        "\nuri=$uri," +
                        "\nmimeType=${fileToReturn.mimeType}"
            }

            ClipData.Item(uri)
        }

        val firstUri = clipDataItems.first().uri

        return Intent().apply {
            // For single selection – data, type and stream.
            // Do not set this for the multiple selection
            // as in this case some apps will discard other files.
            if (filesToReturn.size == 1) {
                setDataAndType(firstUri, filesToReturn.first().mimeType)
                putExtra(Intent.EXTRA_STREAM, firstUri)
            }

            // For multiple selection – ClipData.
            // It is created from the first item, another items are added subsequently.
            clipData = ClipData.newUri(contentResolver, "whatever", firstUri).apply {
                clipDataItems.forEachIndexed { i, clipDataItem ->
                    if (i != 0) {
                        addItem(clipDataItem)
                    }
                }
            }

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * File to return as an activity result.
     *
     * @param file file stored in the storage, accessible by the app content provider.
     * @param mimeType actual MIME-type of the file disregarding the [file] extension.
     * @param displayName actual name of the file disregarding the [file] name.
     */
    data class FileToReturn(
        val file: File,
        val mimeType: String,
        val displayName: String,
    )
}