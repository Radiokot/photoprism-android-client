package com.yqritc.scalablevideoview

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import ua.com.radiokot.photoprism.extension.kLogger

object VideoViewInstanceCache {
    private val log = kLogger("VideoViewInstanceCache")
    private var cache: Triple<Uri, SurfaceTexture, MediaPlayer>? = null

    fun put(mediaUriKey: Uri, surfaceTexture: SurfaceTexture, player: MediaPlayer) {
        log.debug {
            "put(): put:" +
                    "\nkey=$mediaUriKey"
        }

        cache = Triple(mediaUriKey, surfaceTexture, player)
    }

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