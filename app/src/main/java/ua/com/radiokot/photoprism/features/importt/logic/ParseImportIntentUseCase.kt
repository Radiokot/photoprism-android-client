package ua.com.radiokot.photoprism.features.importt.logic

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile

class ParseImportIntentUseCase(
    private val intent: Intent,
    private val contentResolver: ContentResolver,
) {
    private val log = kLogger("ParseImportIntentUseCase")

    operator fun invoke(): List<ImportableFile> {
        check(intent.action in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) {
            "The intent has unsupported action ${intent.action}"
        }

        val uris = mutableSetOf<Uri>()
        intent.data?.also(uris::add)

        @Suppress("DEPRECATION")
        when (intent.action) {
            Intent.ACTION_SEND ->
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    ?.also(uris::add)

            Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    ?.also(uris::addAll)
        }

        check(uris.isNotEmpty()) {
            "The intent has no URIs"
        }

        return uris.mapNotNull { uri ->
            val mimeType: String? = contentResolver.getType(uri)
            val size: Long
            val displayName: String

            val queryCursor = contentResolver.query(
                uri,
                arrayOf(
                    MediaColumns.SIZE,
                    MediaColumns.DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )

            if (queryCursor == null) {
                log.warn {
                    "invoke(): uri_query_failed:" +
                            "\nuri=$uri"
                }

                return@mapNotNull null
            }

            queryCursor.use { cursor ->
                if (!cursor.moveToNext()) {
                    log.warn {
                        "uri_query_is_empty:" +
                                "\nuri=$uri"
                    }

                    return@mapNotNull null
                }

                size =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaColumns.SIZE))
                displayName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME))
            }

            ImportableFile(
                contentUri = uri.toString(),
                mimeType = mimeType,
                size = size,
                displayName = displayName,
            )
        }
    }

    class Factory(
        private val contentResolver: ContentResolver,
    ) {
        fun get(
            intent: Intent,
        ) = ParseImportIntentUseCase(
            contentResolver = contentResolver,
            intent = intent,
        )
    }
}
