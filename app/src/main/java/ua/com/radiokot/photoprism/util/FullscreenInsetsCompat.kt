package ua.com.radiokot.photoprism.util

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ua.com.radiokot.photoprism.R

object FullscreenInsetsCompat {
    /**
     * @return a [Rect] of insets considering default system bar heights
     * on old SDK versions where translucent bars are already available.
     */
    fun getForTranslucentSystemBars(viewToObtainInsets: View): Rect {
        val resources = viewToObtainInsets.resources
        val orientation = resources.configuration.orientation
        val isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        return ViewCompat.getRootWindowInsets(viewToObtainInsets)
            ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
            .let { insets ->
                val left = insets?.left
                    ?: if (Build.VERSION.SDK_INT in
                        (Build.VERSION_CODES.KITKAT..Build.VERSION_CODES.LOLLIPOP_MR1)
                        && orientation == Configuration.ORIENTATION_LANDSCAPE
                        && isRtl
                    )
                        resources.getDimensionPixelSize(R.dimen.default_navigation_bar_height)
                    else
                        0

                val top = insets?.top
                    ?: if (Build.VERSION.SDK_INT in
                        (Build.VERSION_CODES.KITKAT..Build.VERSION_CODES.LOLLIPOP_MR1)
                        && orientation == Configuration.ORIENTATION_PORTRAIT
                    )
                        resources.getDimensionPixelSize(R.dimen.default_status_bar_height)
                    else
                        0

                val right = insets?.right
                    ?: if (Build.VERSION.SDK_INT in
                        (Build.VERSION_CODES.KITKAT..Build.VERSION_CODES.LOLLIPOP_MR1)
                        && orientation == Configuration.ORIENTATION_LANDSCAPE
                        && !isRtl
                    )
                        resources.getDimensionPixelSize(R.dimen.default_navigation_bar_height)
                    else
                        0

                val bottom = insets?.bottom
                    ?: if (Build.VERSION.SDK_INT in
                        (Build.VERSION_CODES.KITKAT..Build.VERSION_CODES.LOLLIPOP_MR1)
                        && orientation == Configuration.ORIENTATION_PORTRAIT
                    )
                        resources.getDimensionPixelSize(R.dimen.default_navigation_bar_height)
                    else
                        0

                Rect(left, top, right, bottom)
            }
    }
}
