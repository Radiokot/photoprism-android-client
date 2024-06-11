package ua.com.radiokot.photoprism.features.importt.view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.MediaColumns
import android.widget.TextView
import ua.com.radiokot.photoprism.base.view.BaseActivity

class ImportDebugActivity : BaseActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uris: Set<Uri> =
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::setOf) ?: emptySet()) +
                    (intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptySet()) +
                    (intent.data?.let(::setOf) ?: emptySet())

        setContentView(TextView(this).apply {
            setPadding(40, 40, 40, 40)
            text =
                """
                    Action: ${intent.action}
                    
                    URIs: ${uris}
                    
                    Describe: ${
                    uris.mapIndexed { index, uri ->
                        "#${index} ${describeFile(uri)}"
                    }
                }
                """.trimIndent()
        })
    }

    private fun describeFile(uri: Uri): String = try {
        val mimeType = contentResolver.getType(uri)

        contentResolver.query(
            uri,
            arrayOf(MediaColumns.DISPLAY_NAME, MediaColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            check(cursor.moveToNext()) {
                "The result is empty"
            }

            val displayNameIndex = cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)

            return """$mimeType ${cursor.getLong(sizeIndex)} ${
                cursor.getString(
                    displayNameIndex
                )
            }"""
        } ?: "failed"
    } catch (e: Exception) {
        e.printStackTrace()
        "failed"
    }
}
