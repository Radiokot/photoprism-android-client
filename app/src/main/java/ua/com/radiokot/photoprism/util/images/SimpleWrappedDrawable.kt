package ua.com.radiokot.photoprism.util.images

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * A [Drawable] that wraps another one and allows its replacement
 *
 * @param defaultWidthPx value to set bounds if the wrapped has no intrinsic width
 * @param defaultHeightPx value to set bounds if the wrapped has no intrinsic height
 *
 * @see wrapped
 */
class SimpleWrappedDrawable(
    private val defaultWidthPx: Int = -1,
    private val defaultHeightPx: Int = -1,
) : Drawable() {
    var wrapped: Drawable? = null
        set(value) {
            field = value

            val width =
                if (value != null && value.intrinsicWidth != -1)
                    value.intrinsicWidth
                else
                    defaultWidthPx

            val height =
                if (value != null && value.intrinsicHeight != -1)
                    value.intrinsicHeight
                else
                    defaultHeightPx

            value?.setBounds(0, 0, width, height)
            setBounds(0, 0, width, height)
        }

    override fun draw(canvas: Canvas) {
        wrapped?.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        wrapped?.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        wrapped?.colorFilter = colorFilter
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int =
        wrapped?.opacity ?: PixelFormat.OPAQUE
}
