package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import android.net.Uri

/**
 * A cache for video players that allows fast players reuse and seamless video view recreation.
 */
interface VideoPlayerCache {
    /**
     * @return cached player or newly created instance in case of miss.
     */
    fun getPlayer(
        mediaSourceUri: Uri,
        context: Context,
    ): VideoPlayer
}