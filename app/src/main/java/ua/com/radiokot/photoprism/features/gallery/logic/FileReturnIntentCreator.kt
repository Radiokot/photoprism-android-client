package ua.com.radiokot.photoprism.features.gallery.logic

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore.MediaColumns
import androidx.core.content.FileProvider
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import java.io.File

/**
 * Creates result/share intent for downloaded files.
 *
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
     * @return a compatible result/share [Intent] returning [fileToReturn]
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
            SendableFile(
                file = fileToReturn,
                mimeType = mimeType,
                displayName = displayName,
            )
        )
    )

    /**
     * @return a compatible result/share [Intent] returning [filesToReturn]
     * with the corresponding [SendableFile.mimeType] and [SendableFile.displayName],
     * assuming that the [FileProvider] of [fileProviderAuthority]
     * supports MIME-type updates.
     */
    fun createIntent(filesToReturn: List<SendableFile>): Intent {
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
        val firstFile = filesToReturn.first()

        return Intent().apply {
            if (filesToReturn.size == 1) {
                // For a single file, the URI is set directly to the intent data.
                // Do not use this for the multiple selection
                // as in this case some apps will discard other files.
                setDataAndType(firstUri, firstFile.mimeType)

                // For a single file, the stream extra is the URI itself.
                putExtra(Intent.EXTRA_STREAM, firstUri)

                // For a single file, it is also possible to set the title.
                putExtra(Intent.EXTRA_TITLE, firstFile.displayName)

                action = Intent.ACTION_SEND
            } else {
                // For multiple files, ClipData is used to return them to the requesting app.
                // It is created from the first item, another items are added subsequently.
                clipData = ClipData.newRawUri("whatever", firstUri).apply {
                    clipDataItems.forEachIndexed { i, clipDataItem ->
                        if (i != 0) {
                            addItem(clipDataItem)
                        }
                    }
                }

                // For multiple files, if they have the same MIME type, it is set.
                // Otherwise, "any" is set – the intent will not launch without it.
                type =
                    if (filesToReturn.distinctBy(SendableFile::mimeType).size == 1)
                        firstFile.mimeType
                    else
                        "*/*"

                // For multiple files, the stream is an ArrayList (not array) of URIs.
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(clipDataItems.map(ClipData.Item::getUri))
                )

                // For multiple files, it is possible to list all the MIME types
                // as a String array (String[], not ArrayList).
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    filesToReturn.map(SendableFile::mimeType).toTypedArray()
                )

                action = Intent.ACTION_SEND_MULTIPLE
            }

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
