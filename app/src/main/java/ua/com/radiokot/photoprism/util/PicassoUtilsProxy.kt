package com.squareup.picasso

import android.content.Context
import java.io.File

object PicassoUtilsProxy {
    fun createDefaultCacheDir(context: Context): File =
        Utils.createDefaultCacheDir(context)

    fun calculateDiskCacheSize(cacheDir: File): Long =
        Utils.calculateDiskCacheSize(cacheDir)
}