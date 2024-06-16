package ua.com.radiokot.photoprism.util.downloader

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer

/**
 * A [ResponseBody] reporting its reading progress.
 *
 * @param onReadingProgress callback for reading progress update
 */
class ReadingProgressResponseBody(
    private val observingBody: ResponseBody,
    private val onReadingProgress: (bytesRead: Long) -> Unit,
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null

    override fun contentLength(): Long =
        observingBody.contentLength()

    override fun contentType(): MediaType? =
        observingBody.contentType()

    override fun source(): BufferedSource =
        bufferedSource
            ?: ReadingProgressSource(
                delegate = observingBody.source(),
                onReadingProgress = onReadingProgress,
            )
                .buffer()
                .also { bufferedSource = it }
}
