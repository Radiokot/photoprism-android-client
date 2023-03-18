package ua.com.radiokot.photoprism.util.downloader

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import okhttp3.Request
import okio.Sink
import ua.com.radiokot.photoprism.di.HttpClient
import ua.com.radiokot.photoprism.extension.checkNotNull
import java.util.concurrent.atomic.AtomicInteger

class OkHttpObservableDownloader(
    httpClient: HttpClient,
) : ObservableDownloader {
    private val requestCounter = AtomicInteger(0)
    private val emittersMap: MutableMap<Int, ObservableEmitter<ObservableDownloader.Progress>> =
        mutableMapOf()

    private val observableHttpClient =
        httpClient
            .newBuilder()
            .addNetworkInterceptor { chain ->
                val emitterKey = chain.request().tag() as Int
                val originalResponse = chain.proceed(chain.request())
                originalResponse
                    .newBuilder()
                    .body(
                        ProgressResponseBody(
                            originalResponse.body
                                .checkNotNull {
                                    "The request must have a body, otherwise there is nothing to download"
                                }
                        ) { bytesRead, contentLength, isDone ->
                            val emitter = emittersMap.getValue(emitterKey)
                            if (isDone) {
                                emitter.onComplete()
                            } else {
                                emitter.onNext(
                                    ObservableDownloader.Progress(
                                        bytesRead = bytesRead,
                                        contentLength = contentLength,
                                    )
                                )
                            }
                        }
                    )
                    .build()
            }
            .build()

    override fun download(
        url: String,
        destination: Sink
    ): Observable<ObservableDownloader.Progress> = Observable.create { emitter ->
        val emitterKey = requestCounter.incrementAndGet()

        val request = Request.Builder()
            .get()
            .url(url)
            .tag(emitterKey)
            .build()
        emittersMap[emitterKey] = emitter

        observableHttpClient
            .newCall(request)
            .execute()
            .body
            .checkNotNull {
                "The request must have a body, otherwise there is nothing to download"
            }
            .source()
            .readAll(destination)
    }
}