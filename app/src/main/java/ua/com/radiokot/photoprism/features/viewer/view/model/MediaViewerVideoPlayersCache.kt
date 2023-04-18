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

/**
 * A view model that maintains a singleton cache of video players,
 * for fast swiping and seamless view recreation (e.g. on activity recreation).
 */
class MediaViewerVideoPlayersCache : ViewModel() {
    private val log = kLogger("MediaViewerVPCacheVM")

    init {
        playersCache.evictAll()
        log.debug { "init(): cleared_cache" }
    }

    fun touch() {
    }

    override fun onCleared() {
        playersCache.evictAll()
        log.debug { "onCleared(): cleared_cache" }
    }

    companion object {
        private val log = kLogger("MediaViewerVPCache")

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

        fun get(
            mediaSourceUri: Uri,
            context: Context,
        ): ExoPlayer =
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
    }
}