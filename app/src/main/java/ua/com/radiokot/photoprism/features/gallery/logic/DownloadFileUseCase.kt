package ua.com.radiokot.photoprism.features.gallery.logic

import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import java.io.File

class DownloadFileUseCase(
    private val url: String,
    private val destination: File,
    private val observableDownloader: ObservableDownloader,
) {
    fun perform(): Observable<ObservableDownloader.Progress> {
        return observableDownloader.download(
            url = url,
            destination = destination,
        )
    }

    class Factory(
        private val observableDownloader: ObservableDownloader,
    ) {
        fun get(
            url: String,
            destination: File,
        ) =
            DownloadFileUseCase(
                url = url,
                destination = destination,
                observableDownloader = observableDownloader,
            )
    }
}