package ua.com.radiokot.photoprism.features.viewer.view.model

import androidx.lifecycle.ViewModel
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.viewer.logic.VideoPlayer
import ua.com.radiokot.photoprism.features.viewer.logic.VideoPlayerCache
import ua.com.radiokot.photoprism.features.viewer.logic.VideoPlayerFactory

/**
 * A view model that implements activity-scoped [VideoPlayerCache] based on a map.
 * The players must be released manually by [releasePlayer]
 */
class VideoPlayerCacheViewModel(
    private val videoPlayerFactory: VideoPlayerFactory,
) : ViewModel(), VideoPlayerCache {
    private val log = kLogger("VideoPlayerCacheVM")
    private val cache = mutableMapOf<Any, VideoPlayer>()

    override fun getPlayer(key: Any): VideoPlayer {
        return getCachedPlayer(key)
            ?: createAndCacheMissingPlayer(key)
    }

    private fun getCachedPlayer(key: Any): VideoPlayer? =
        cache[key]
            ?.also { player ->
                log.debug {
                    "getCachedPlayer(): cache_hit:" +
                            "\nkey=$key," +
                            "\nplayer=$player"
                }
            }

    private fun createAndCacheMissingPlayer(key: Any): VideoPlayer =
        videoPlayerFactory.createVideoPlayer()
            .also { createdPlayer ->
                cache[key] = createdPlayer

                log.debug {
                    "createAndCacheMissingPlayer(): cached_created_player:" +
                            "\nkey=$key," +
                            "\nplayer=$createdPlayer," +
                            "\ncacheSize=${cache.size}"
                }
            }

    override fun releasePlayer(key: Any) {
        cache[key]?.also { player ->
            player.release()
            cache.remove(key)

            log.debug {
                "releasePlayer(): released_player:" +
                        "\nkey=$key," +
                        "\nplayer=$player," +
                        "\ncacheSize=${cache.size}"
            }
        }
    }

    override fun onCleared() {
        cache.values.forEach(VideoPlayer::release)
        cache.clear()
        log.debug { "onCleared(): cleared_cache" }
    }
}
