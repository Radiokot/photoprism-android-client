package ua.com.radiokot.photoprism.util.downloader

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import okhttp3.Request
import okhttp3.Response
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
                val originalBody = originalResponse.body
                    ?: return@addNetworkInterceptor originalResponse
                val contentLength = originalBody.contentLength()

                return@addNetworkInterceptor originalResponse
                    .newBuilder()
                    .body(
                        ReadingProgressResponseBody(
                            observingBody = originalBody,
                            onReadingProgress = { bytesRead: Long ->
                                val emitter = emittersMap.getValue(emitterKey)
                                emitter.onNext(
                                    ObservableDownloader.Progress(
                                        bytesRead = bytesRead,
                                        contentLength = contentLength,
                                    )
                                )
                            },
                        )
                    )
                    .build()
            }
            .build()

    override fun download(
        url: String,
        destination: Sink
    ): Observable<ObservableDownloader.Progress> {
        val emitterKey = requestCounter.incrementAndGet()

        val call = observableHttpClient.newCall(
            Request.Builder()
                .get()
                .url(url)
                .tag(emitterKey)
                .build()
        )

        return Observable.create { emitter ->
            emittersMap[emitterKey] = emitter

            call.execute().use { response ->
                try {
                    response
                        .takeIf(Response::isSuccessful)
                        .checkNotNull {
                            "The response must be successful, no sense in downloading an error"
                        }
                        .body
                        .checkNotNull {
                            "The response must have a body, otherwise there is nothing to download"
                        }
                        .source()
                        .readAll(destination)

                    emitter.onComplete()
                } catch (t: Throwable) {
                    emitter.tryOnError(t)
                }
            }
        }.doOnDispose(call::cancel)
    }
}
