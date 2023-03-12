package ua.com.radiokot.photoprism.util.downloader

import io.reactivex.rxjava3.core.Observable
import okio.Sink
import okio.sink
import java.io.File

interface ObservableDownloader {
    /**
     * Downloads content of the [url] to the given [destination].
     *
     * @return cold [Observable] that reports [Progress] and completes when the download is ended.
     */
    fun download(
        url: String,
        destination: Sink,
    ): Observable<Progress>

    /**
     * Downloads content of the [url] to the given [destination].
     *
     * @return cold [Observable] that reports [Progress] and completes when the download is ended.
     */
    fun download(
        url: String,
        destination: File,
        append: Boolean = false,
    ): Observable<Progress> =
        download(url, destination.sink(append))

    class Progress(
        /**
         * Number of total bytes read.
         */
        val bytesRead: Long,

        /**
         * Content length reported by the server,
         * may be -1.
         */
        val contentLength: Long,
    ) {
        /**
         * Progress percent from 0 to 100,
         * or -1 if there is no [contentLength]
         */
        val percent: Double
            get() = if (contentLength <= 0L)
                -1.0
            else
                (bytesRead.toDouble() / contentLength) * 100
    }
}