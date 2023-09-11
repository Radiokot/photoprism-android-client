package ua.com.radiokot.photoprism.view

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.color.MaterialColors

/**
 * A [SwipeRefreshLayout] which is aware of current theme attributes.
 *
 * - Progress drawable color is set to the theme primary color;
 */
class ThemedSwipeRefreshLayout
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs) {

    init {
        setColorSchemeColors(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimary,
            )
        )
    }
}
