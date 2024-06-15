package ua.com.radiokot.photoprism.features.importt.model

import android.content.ContentResolver
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.BufferedSink

class ImportableFileRequestBody(
    private val importableFile: ImportableFile,
    private val contentResolver: ContentResolver,
) : RequestBody() {
    override fun contentLength(): Long =
        importableFile.size

    override fun contentType(): MediaType? =
        importableFile.mimeType?.toMediaTypeOrNull()

    override fun writeTo(sink: BufferedSink) {
        val fileSource = importableFile.source(contentResolver)
        try {
            sink.writeAll(fileSource)
        } finally {
            fileSource.closeQuietly()
        }
    }
}
