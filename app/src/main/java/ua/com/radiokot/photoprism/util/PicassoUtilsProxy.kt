package com.squareup.picasso

import java.io.File

object PicassoUtilsProxy {
    fun calculateDiskCacheSize(cacheDir: File): Long =
        Utils.calculateDiskCacheSize(cacheDir)
}