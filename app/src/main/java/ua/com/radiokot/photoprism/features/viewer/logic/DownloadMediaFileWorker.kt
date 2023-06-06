package ua.com.radiokot.photoprism.features.viewer.logic

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.core.Single
import okio.blackholeSink
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader

class DownloadMediaFileWorker(
    context: Context,
    workerParams: WorkerParameters,
) : RxWorker(context, workerParams), KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val downloader: ObservableDownloader by inject()

    override fun createWork(): Single<Result> =
        downloader.download(
            url = "https://sample-videos.com/img/Sample-jpg-image-30mb.jpg",
            destination = blackholeSink(),
        )
            .doOnNext { progress ->
                println("OOLEG progress ${progress.percent}")
            }
            .ignoreElements()
            .toSingleDefault(Result.success())
}
