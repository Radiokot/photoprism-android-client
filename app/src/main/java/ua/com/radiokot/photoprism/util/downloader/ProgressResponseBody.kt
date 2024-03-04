package ua.com.radiokot.photoprism.util.downloader

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

typealias DownloadProgressListener = (read: Long, length: Long) -> Unit

/**
 * @param progressListener called on each progress update
 */
class ProgressResponseBody(
    private val observingBody: ResponseBody,
    private val progressListener: DownloadProgressListener,
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null

    override fun contentLength(): Long =
        observingBody.contentLength()

    override fun contentType(): MediaType? =
        observingBody.contentType()

    override fun source(): BufferedSource =
        bufferedSource
            ?: wrapSource(observingBody.source())
                .buffer()
                .also { bufferedSource = it }

    private fun wrapSource(source: Source): Source = object : ForwardingSource(source) {
        private var totalBytesRead: Long = 0L

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            totalBytesRead += if (bytesRead != -1L) bytesRead else 0

            progressListener(totalBytesRead, contentLength())

            return bytesRead
        }
    }
}
