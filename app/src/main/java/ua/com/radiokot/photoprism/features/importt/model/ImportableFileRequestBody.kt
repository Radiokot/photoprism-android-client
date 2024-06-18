package ua.com.radiokot.photoprism.features.importt.model

import android.content.ContentResolver
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import ua.com.radiokot.photoprism.util.downloader.ReadingProgressSource

class ImportableFileRequestBody(
    private val importableFile: ImportableFile,
    private val contentResolver: ContentResolver,
    private val onReadingProgress: (bytesRead: Long) -> Unit,
) : RequestBody() {
    override fun contentLength(): Long =
        importableFile.size

    override fun contentType(): MediaType? =
        importableFile.mimeType?.toMediaTypeOrNull()

    override fun writeTo(sink: BufferedSink) {
        val fileSource = ReadingProgressSource(
            delegate = importableFile.source(contentResolver),
            onReadingProgress = onReadingProgress,
        )

        try {
            sink.writeAll(fileSource)
        } finally {
            fileSource.closeQuietly()
        }
    }
}
