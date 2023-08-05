package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import okhttp3.CacheControl
import ua.com.radiokot.photoprism.di.HttpClient

class DefaultVideoPlayerFactory(
    private val httpClient: HttpClient,
    private val sharedCache: Cache,
    private val context: Context,
) : VideoPlayerFactory {
    override fun createVideoPlayer(): VideoPlayer =
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        6_000,
                        30_000,
                        500,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .build()
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    CacheDataSource.Factory()
                        .setCache(sharedCache)
                        .setUpstreamDataSourceFactory(
                            OkHttpDataSource.Factory(httpClient)
                                .setCacheControl(
                                    CacheControl.Builder()
                                        // Assumption: PhotoPrism content identified by hash is immutable.
                                        // I'm not sure if it is used by video player own cache.
                                        .immutable()
                                        .build()
                                )
                        )
                )
            )
            .build()
}
