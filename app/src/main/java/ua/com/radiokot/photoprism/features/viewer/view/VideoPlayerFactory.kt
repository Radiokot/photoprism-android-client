package ua.com.radiokot.photoprism.features.viewer.view

fun interface VideoPlayerFactory {
    /**
     * Creates and initially sets up a video player.
     */
    fun createVideoPlayer(): VideoPlayer
}