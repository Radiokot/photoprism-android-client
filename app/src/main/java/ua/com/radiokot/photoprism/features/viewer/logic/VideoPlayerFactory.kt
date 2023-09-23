package ua.com.radiokot.photoprism.features.viewer.logic

fun interface VideoPlayerFactory {
    /**
     * Creates and initially sets up a video player.
     */
    fun createVideoPlayer(): VideoPlayer
}
