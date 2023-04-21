package ua.com.radiokot.photoprism.features.viewer.view.model

import android.content.Context
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.ViewModel
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayer
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerCache
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerFactory

/**
 * A view model that implements activity-scoped [VideoPlayerCache] based on an [LruCache].
 *
 * @param cacheMaxSize max number of cached players,
 * should not be less then a number of simultaneously visible players.
 * 2 is enough for pages swiping
 */
class VideoPlayerCacheViewModel(
    private val videoPlayerFactory: VideoPlayerFactory,
    cacheMaxSize: Int,
) : ViewModel(), VideoPlayerCache {
    private val log = kLogger("VideoPlayerCacheVM")
    private val playersCache = object : LruCache<Int, VideoPlayer>(cacheMaxSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: Int,
            oldValue: VideoPlayer,
            newValue: VideoPlayer?
        ) {
            oldValue.release()
            log.debug {
                "entryRemoved(): released_player:" +
                        "\nkey=$key," +
                        "\nplayer=$oldValue"
            }
        }
    }

    override fun getPlayer(mediaSourceUri: Uri, context: Context): VideoPlayer {
        // Do not use URI as a key, as it may contain HTTP auth credentials.
        val key = mediaSourceUri.hashCode()

        return getCachedPlayer(key)
            ?: createAndCacheMissingPlayer(key, mediaSourceUri)
    }

    private fun getCachedPlayer(key: Int): VideoPlayer? =
        playersCache[key]
            ?.also { player ->
                log.debug {
                    "get(): cache_hit:" +
                            "\nkey=$key," +
                            "\nplayer=$player"
                }
            }

    private fun createAndCacheMissingPlayer(
        key: Int,
        mediaSourceUri: Uri
    ): VideoPlayer =
        videoPlayerFactory.createVideoPlayer(mediaSourceUri = mediaSourceUri)
            .also { createdPlayer ->
                playersCache.put(key, createdPlayer)

                log.debug {
                    "get(): cache_miss:" +
                            "\nkey=$key," +
                            "\ncreatedPlayer=$createdPlayer"
                }
            }

    override fun onCleared() {
        playersCache.evictAll()
        log.debug { "onCleared(): cleared_cache" }
    }
}