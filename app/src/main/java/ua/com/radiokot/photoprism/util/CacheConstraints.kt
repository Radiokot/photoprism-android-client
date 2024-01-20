package ua.com.radiokot.photoprism.util

import com.squareup.picasso.PicassoUtilsProxy
import java.io.File

object CacheConstraints {
    /**
     * @return optimal disk cache size in bytes considering device storage capabilities.
     */
    fun getOptimalSize(cacheDir: File): Long =
        // I'm OK with Picasso implementation.
        PicassoUtilsProxy.calculateDiskCacheSize(cacheDir)
}
