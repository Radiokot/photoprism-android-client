package ua.com.radiokot.photoprism.util.images

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.squareup.picasso.Transformation

object ImageTransformations {
    val circle: Transformation by lazy {
        ShapeMaskImageTransformation(object : ShapeMaskImageTransformation.ShapeMask {
            override val name: String =
                "circle"

            override fun getRect(sourceWidth: Int, sourceHeight: Int): Rect =
                ShapeMaskImageTransformation.ShapeMask.getCenterSquareRect(
                    sourceWidth,
                    sourceHeight
                )

            override fun draw(canvas: Canvas, paint: Paint) {
                val radius = canvas.width / 2f
                canvas.drawCircle(
                    radius,
                    radius,
                    radius,
                    paint
                )
            }
        })
    }
}
