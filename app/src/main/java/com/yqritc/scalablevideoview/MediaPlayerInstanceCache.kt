package com.yqritc.scalablevideoview

import android.media.MediaPlayer
import android.net.Uri

interface MediaPlayerInstanceCache {
    fun put(mediaUriKey: Uri, instance: MediaPlayer)
    fun get(mediaUriKey: Uri): MediaPlayer?
}