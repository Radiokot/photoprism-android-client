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

        val uris: Set<Uri> = (intent.data?.let(::setOf) ?: emptySet()) +
                (intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptySet()) +
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::setOf) ?: emptySet())

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
                    "invoke(): cant_query_uri:" +
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
