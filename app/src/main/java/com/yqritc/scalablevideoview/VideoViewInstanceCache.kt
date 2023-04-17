package com.yqritc.scalablevideoview

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import ua.com.radiokot.photoprism.extension.kLogger

object VideoViewInstanceCache {
    private val log = kLogger("VideoViewInstanceCache")
    private var cache: Triple<Uri, SurfaceTexture, MediaPlayer>? = null

    fun put(mediaUriKey: Uri, surfaceTexture: SurfaceTexture, player: MediaPlayer) {
        if (cache?.first != mediaUriKey) {
            release()
        }

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

    fun release() {
        if (cache != null) {
            cache?.second?.release()
            cache?.third?.release()

            log.debug { "release(): released_instances" }
        }
    }
}