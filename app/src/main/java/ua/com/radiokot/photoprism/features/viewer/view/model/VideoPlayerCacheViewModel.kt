package ua.com.radiokot.photoprism.features.viewer.view.model

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

    override fun getPlayer(key: Any): VideoPlayer {
        val cacheKey = key.hashCode()

        return getCachedPlayer(cacheKey)
            ?: createAndCacheMissingPlayer(cacheKey)
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

    private fun createAndCacheMissingPlayer(key: Int): VideoPlayer =
        videoPlayerFactory.createVideoPlayer()
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