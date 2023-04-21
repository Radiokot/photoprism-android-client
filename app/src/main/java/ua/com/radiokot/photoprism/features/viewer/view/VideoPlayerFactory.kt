package ua.com.radiokot.photoprism.features.viewer.view

import android.net.Uri

fun interface VideoPlayerFactory {
    /**
     * Creates and sets up a video player to play the media
     * described by [mediaSourceUri]
     */
    fun createVideoPlayer(mediaSourceUri: Uri): VideoPlayer
}