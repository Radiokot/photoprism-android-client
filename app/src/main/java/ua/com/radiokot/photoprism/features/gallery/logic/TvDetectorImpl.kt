package ua.com.radiokot.photoprism.features.gallery.logic

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

class TvDetectorImpl(
    context: Context,
) : TvDetector {
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val packageManager = context.packageManager

    @Suppress("DEPRECATION")
    override val isRunningOnTv: Boolean
        get() = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
                || packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
}
