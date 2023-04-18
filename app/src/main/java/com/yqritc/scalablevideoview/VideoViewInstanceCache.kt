package com.yqritc.scalablevideoview

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import ua.com.radiokot.photoprism.extension.kLogger

/**
 * This is a cache for VideoView playing instances, to be used
 * for seamless view recreation (e.g. during the orientation change)
 */
object VideoViewInstanceCache {
    private val log = kLogger("VideoViewInstanceCache")
    private var cache: Triple<Uri, SurfaceTexture, MediaPlayer>? = null

    /**
     * Indicates whether the VideoView should cache the instances.
     */
    @JvmStatic
    var isEnabled: Boolean = false

    @JvmStatic
    fun put(mediaUriKey: Uri, surfaceTexture: SurfaceTexture, player: MediaPlayer) {
        if (!isEnabled) {
            return
        }

        log.debug {
            "put(): put:" +
                    "\nkey=$mediaUriKey"
        }

        cache = Triple(mediaUriKey, surfaceTexture, player)
    }

    @JvmStatic
    fun get(mediaUriKey: Uri): Pair<SurfaceTexture, MediaPlayer>? =
        cache
            ?.takeIf { it.first == mediaUriKey }
            ?.let { it.second to it.third }
            .also { hit ->
                if (hit != null) {
                    log.debug {
                        "get(): hit:" +
                                "\nkey=$mediaUriKey"
                    }
                } else {
                    log.debug {
                        "get(): miss:" +
                                "\nkey=$mediaUriKey"
                    }
                }
            }

    /**
     * Clears the cache and releases resources.
     * Be careful not to call it after a cache hit, as it will
     * free resources of instances currently in use.
     */
    fun clearAndRelease() {
        if (cache != null) {
            cache?.second?.release()
            cache?.third?.release()
            cache = null

            log.debug { "clearAndRelease(): released_instances" }
        } else {
            log.debug { "clearAndRelease(): nothing_to_clear" }
        }
    }
}