package ua.com.radiokot.photoprism.features.viewer.view

/**
 * A cache for video players that allows fast players reuse and seamless video view recreation.
 */
interface VideoPlayerCache {
    /**
     * @return cached player or newly created instance in case of miss.
     *
     * @param key an object with consistent hash code
     */
    fun getPlayer(
        key: Any,
    ): VideoPlayer
}