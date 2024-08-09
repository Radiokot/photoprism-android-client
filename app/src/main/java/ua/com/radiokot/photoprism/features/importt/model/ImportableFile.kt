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
    val size: Long,
) {
    @SuppressLint("Recycle")
    fun source(contentResolver: ContentResolver): Source =
        contentResolver.openInputStream(contentUri.toUri())
            .checkNotNull { "Can't open input stream for $contentUri" }
            .source()
}

val Iterable<ImportableFile>.sizeMb: Double
    get() = sumOf(ImportableFile::size).toDouble() / (1024 * 1024)
