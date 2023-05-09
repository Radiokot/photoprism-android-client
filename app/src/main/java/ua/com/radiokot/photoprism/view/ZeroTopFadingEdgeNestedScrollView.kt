package ua.com.radiokot.photoprism.view

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

class ZeroTopFadingEdgeNestedScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : NestedScrollView(context, attrs) {
    override fun getTopFadingEdgeStrength(): Float = 0f
}