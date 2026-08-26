package ua.com.radiokot.photoprism.extension

import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import kotlin.math.roundToInt

fun MaterialToolbar.barsAndCutoutPadding() =
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val safe = insets.barsAndCutout()
        val top = safe.top
        updatePadding(
            top =
                if (top == 0)
                    0
                else
                    top + (resources.displayMetrics.density * 4).roundToInt(),
            left = safe.left,
            right = safe.right,
            bottom =
                if (safe.top == 0)
                    0
                else
                    (resources.displayMetrics.density * 4).roundToInt(),
        )
        insets
    }
