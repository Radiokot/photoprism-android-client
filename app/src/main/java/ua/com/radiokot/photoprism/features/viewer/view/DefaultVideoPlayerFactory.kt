package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import okhttp3.CacheControl
import ua.com.radiokot.photoprism.di.HttpClient

class DefaultVideoPlayerFactory(
    private val httpClient: HttpClient,
    private val context: Context,
) : VideoPlayerFactory {
    override fun createVideoPlayer(): VideoPlayer =
        ExoPlayer.Builder(context)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    OkHttpDataSource.Factory(httpClient)
                        .setCacheControl(
                            CacheControl.Builder()
                                // Assumption: PhotoPrism content identified by hash is immutable.
                                .immutable()
                                .build()
                        )
                )
            )
            .build()
}