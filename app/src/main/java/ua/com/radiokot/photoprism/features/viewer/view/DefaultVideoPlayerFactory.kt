package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import ua.com.radiokot.photoprism.di.HttpClient

class DefaultVideoPlayerFactory(
    private val httpClient: HttpClient,
    private val context: Context,
) : VideoPlayerFactory {
    override fun createVideoPlayer(mediaSourceUri: Uri): VideoPlayer =
        ExoPlayer.Builder(context)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(httpClient)))
            .build()
            .apply {
                setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(mediaSourceUri.toString())
                        .setUri(mediaSourceUri)
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build()
                )
            }
}