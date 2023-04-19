package ua.com.radiokot.photoprism.features.viewer.view.model

import android.content.Context
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.util.MimeTypes
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerCache

/**
 * A view model that implements activity-scoped [VideoPlayerCache].
 */
class VideoPlayerCacheViewModel : ViewModel(), VideoPlayerCache {
    private val log = kLogger("VideoPlayerCacheVM")
    private val playersCache = object : LruCache<Uri, ExoPlayer>(2) {
        override fun entryRemoved(
            evicted: Boolean,
            key: Uri,
            oldValue: ExoPlayer?,
            newValue: ExoPlayer?
        ) {
            oldValue?.release()
            log.debug {
                "entryRemoved(): released_player:" +
                        "\nkey=$key"
            }
        }
    }

    override fun getPlayer(mediaSourceUri: Uri, context: Context): ExoPlayer =
        playersCache[mediaSourceUri]
            ?.also {
                log.debug {
                    "get(): cache_hit:" +
                            "\nkey=$mediaSourceUri"
                }
            } ?: ExoPlayer.Builder(context)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .build()
            .apply {
                setMediaItem(
                    MediaItem.Builder()
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .setMediaId(mediaSourceUri.toString())
                        .setUri(mediaSourceUri)
                        .build()
                )

                log.debug {
                    "get(): cache_miss:" +
                            "\nkey=$mediaSourceUri"
                }

                playersCache.put(mediaSourceUri, this)
            }

    override fun onCleared() {
        playersCache.evictAll()
        log.debug { "onCleared(): cleared_cache" }
    }
}