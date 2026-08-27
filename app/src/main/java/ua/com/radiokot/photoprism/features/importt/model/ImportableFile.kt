package ua.com.radiokot.photoprism.features.importt.model

import android.annotation.SuppressLint
import android.content.ContentResolver
import androidx.core.net.toUri
import com.fasterxml.jackson.annotation.JsonCreator
import okio.Source
import okio.source
import ua.com.radiokot.photoprism.extension.checkNotNull

data class ImportableFile
@JsonCreator
constructor(
    val contentUri: String,
    val displayName: String,
    val mimeType: String?,

    /**
     * Size reported by the content provider,
     * which can't be trusted.
     */
    val reportedSize: Long?,
) {
    init {
        require(reportedSize == null || reportedSize > 0) {
            "Reported size, if defined, must be positive"
        }
    }

    @SuppressLint("Recycle")
    fun source(contentResolver: ContentResolver): Source =
        contentResolver.openInputStream(contentUri.toUri())
            .checkNotNull { "Can't open input stream for $contentUri" }
            .source()
}

val Iterable<ImportableFile>.reportedSizeMb: Double
    get() =
        asSequence()
            .mapNotNull(ImportableFile::reportedSize)
            .sumOf { it.toDouble() / (1024 * 1024) }
