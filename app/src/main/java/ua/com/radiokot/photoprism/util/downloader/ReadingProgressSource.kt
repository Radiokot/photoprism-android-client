package ua.com.radiokot.photoprism.util.downloader

import okio.Buffer
import okio.ForwardingSource
import okio.Source

/**
 * A [Source] reporting its reading progress.
 *
 * @param onReadingProgress callback for reading progress update
 */
class ReadingProgressSource(
    delegate: Source,
    private val onReadingProgress: (bytesRead: Long) -> Unit,
) : ForwardingSource(delegate) {
    private var totalBytesRead: Long = 0L

    override fun read(sink: Buffer, byteCount: Long): Long {
        val bytesRead = super.read(sink, byteCount)
        totalBytesRead += if (bytesRead != -1L) bytesRead else 0

        onReadingProgress(totalBytesRead)

        return bytesRead
    }
}
