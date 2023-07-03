package ua.com.radiokot.photoprism.features.gallery.logic

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import androidx.core.content.FileProvider
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.kLogger

/**
 * A [FileProvider] that allows custom MIME-type updates for created URIs.
 */
class UpdatableFileProvider : FileProvider(R.xml.file_provider_paths) {
    private val log = kLogger("UpdatableFileProvider")

    private val mimeTypeMap = mutableMapOf<Uri, String>()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return if (values != null && values.containsKey(MediaColumns.MIME_TYPE)) {
            val mimeType = values.getAsString(MediaColumns.MIME_TYPE)
            mimeTypeMap[uri] = mimeType

            log.debug {
                "update(): updated_mime_type:" +
                        "\nuri=$uri," +
                        "\nmimeType=$mimeType"
            }

            1
        } else {
            0
        }
    }

    override fun getType(uri: Uri): String? {
        val updatedMimeType = mimeTypeMap[uri]

        if (updatedMimeType != null) {
            log.debug {
                "getType(): return_updated_mime_type:" +
                        "\nuri=$uri," +
                        "\nmimeType=$updatedMimeType"
            }

            return updatedMimeType
        } else {
            log.debug {
                "getType(): no_updated_mime_type_found:" +
                        "\nuri=$uri"
            }

            return super.getType(uri)
        }
    }
}
