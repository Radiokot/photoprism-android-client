package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import com.google.android.exoplayer2.C
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
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
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