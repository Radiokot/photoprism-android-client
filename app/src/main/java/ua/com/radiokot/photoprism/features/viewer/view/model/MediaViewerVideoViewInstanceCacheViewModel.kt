package ua.com.radiokot.photoprism.features.viewer.view.model

import androidx.lifecycle.ViewModel
import com.yqritc.scalablevideoview.VideoViewInstanceCache
import ua.com.radiokot.photoprism.extension.kLogger

/**
 * This ViewModel ensures that [VideoViewInstanceCache] is used for video views
 * when the activity gets destroyed.
 */
class MediaViewerVideoViewInstanceCacheViewModel : ViewModel() {
    private val log = kLogger("MediaViewerVVCacheVM")

    init {
        VideoViewInstanceCache.clearAndRelease()
        log.debug { "init(): clearedCache" }
    }

    fun touch() {
    }

    fun onPreActivityDestroy() {
        VideoViewInstanceCache.isEnabled = true
        log.debug { "onActivityDestroy(): enabled_cache" }
    }

    override fun onCleared() {
        VideoViewInstanceCache.isEnabled = false
        log.debug { "onCleared(): disabled_cache" }
    }
}