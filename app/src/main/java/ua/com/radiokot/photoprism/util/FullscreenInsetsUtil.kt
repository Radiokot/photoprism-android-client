package ua.com.radiokot.photoprism.util

import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ua.com.radiokot.photoprism.R

object FullscreenInsetsUtil {
    /**
     * @return height of navigation bar overlay in fullscreen
     */
    fun getNavigationBarOverlayHeight(viewToObtainInsets: View): Int {
        return ViewCompat.getRootWindowInsets(viewToObtainInsets)
            ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            ?.bottom
            .let { height ->
                height
                    ?: if (Build.VERSION.SDK_INT in
                        (Build.VERSION_CODES.LOLLIPOP..Build.VERSION_CODES.LOLLIPOP_MR1)
                    )
                        viewToObtainInsets.context.resources.getDimensionPixelSize(R.dimen.default_navigation_bar_height)
                    else
                        0
            }
    }
}