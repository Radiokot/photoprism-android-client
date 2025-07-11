package ua.com.radiokot.photoprism.features.viewer.logic

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.CacheControl
import ua.com.radiokot.photoprism.di.HttpClient

@OptIn(UnstableApi::class)
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
                    .fixClippingSources()
            )
            .build()
}
