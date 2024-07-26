package ua.com.radiokot.photoprism.util.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

class SquareDrawableShapeMask(
    override val name: String,
    private val shapeMaskDrawable: Drawable,
): ShapeMaskImageTransformation.ShapeMask {

    constructor(
        name: String,
        @DrawableRes
        shapeMaskDrawableId: Int,
        context: Context,
    ): this(
        name = name,
        shapeMaskDrawable = ContextCompat.getDrawable(context, shapeMaskDrawableId)!!,
    )

    override fun getRect(sourceWidth: Int, sourceHeight: Int): Rect =
        ShapeMaskImageTransformation.ShapeMask.getCenterSquareRect(
            sourceWidth,
            sourceHeight
        )

    override fun draw(canvas: Canvas, paint: Paint){
        val alphaBitmap = shapeMaskDrawable.toBitmap(
            canvas.width,
            canvas.height,
            Bitmap.Config.ALPHA_8
        )
        canvas.drawBitmap(alphaBitmap, 0f, 0f, paint)
        alphaBitmap.recycle()
    }
}
