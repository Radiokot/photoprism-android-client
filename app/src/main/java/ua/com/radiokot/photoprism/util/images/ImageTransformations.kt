package ua.com.radiokot.photoprism.util.images

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.DrawableRes
import com.squareup.picasso.Transformation
import ua.com.radiokot.photoprism.R
import kotlin.math.min

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

    val vSauce: Transformation by lazy {
        ShapeMaskImageTransformation(object : ShapeMaskImageTransformation.ShapeMask {
            override val name: String =
                "vSauce"

            override fun getRect(sourceWidth: Int, sourceHeight: Int): Rect {
                val circleDiameter = min(sourceWidth, sourceHeight)
                val height = sourceHeight.coerceAtLeast(circleDiameter)
                val horizontalMargin = (sourceWidth - circleDiameter) / 2
                return Rect(
                    horizontalMargin,
                    0,
                    horizontalMargin + circleDiameter,
                    height
                )
            }

            override fun draw(canvas: Canvas, paint: Paint) =
                drawSauce(canvas, paint)
        })
    }

    val hSauce: Transformation by lazy {
        ShapeMaskImageTransformation(object : ShapeMaskImageTransformation.ShapeMask {
            override val name: String =
                "hSauce"

            override fun getRect(sourceWidth: Int, sourceHeight: Int): Rect {
                val circleDiameter = min(sourceWidth, sourceHeight)
                val width = sourceWidth.coerceAtLeast(circleDiameter)
                val verticalMargin = (sourceHeight - circleDiameter) / 2
                return Rect(
                    0,
                    verticalMargin,
                    width,
                    verticalMargin + circleDiameter
                )
            }

            override fun draw(canvas: Canvas, paint: Paint) =
                drawSauce(canvas, paint)
        })
    }

    fun sasha(context: Context) =
        getSquareShapeMaskDrawableTransformation(
            name = "sasha",
            shapeMaskDrawableId = R.drawable.image_shape_sasha,
            context = context,
        )

    fun buba(context: Context) =
        getSquareShapeMaskDrawableTransformation(
            name = "buba",
            shapeMaskDrawableId = R.drawable.image_shape_buba,
            context = context,
        )

    fun fufa(context: Context) =
        getSquareShapeMaskDrawableTransformation(
            name = "fufa",
            shapeMaskDrawableId = R.drawable.image_shape_fufa,
            context = context,
        )

    fun nina(context: Context) =
        getSquareShapeMaskDrawableTransformation(
            name = "nina",
            shapeMaskDrawableId = R.drawable.image_shape_nina,
            context = context,
        )

    fun gear(context: Context) =
        getSquareShapeMaskDrawableTransformation(
            name = "gear",
            shapeMaskDrawableId = R.drawable.image_shape_gear,
            context = context,
        )

    fun leaf(context: Context) =
        getSquareShapeMaskDrawableTransformation(
            name = "leaf",
            shapeMaskDrawableId = R.drawable.image_shape_leaf,
            context = context,
        )

    fun heart(context: Context) =
        getSquareShapeMaskDrawableTransformation(
            name = "heart",
            shapeMaskDrawableId = R.drawable.image_shape_heart,
            context = context,
        )

    fun roundedCorners(
        cornerRadiusDp: Int,
        context: Context,
    ) = ShapeMaskImageTransformation(object : ShapeMaskImageTransformation.ShapeMask {
        private val cornerRadiusPx =
            cornerRadiusDp * context.resources.displayMetrics.density

        override val name: String =
            "roundedCorners-$cornerRadiusDp"

        override fun getRect(sourceWidth: Int, sourceHeight: Int): Rect =
            Rect(0, 0, sourceWidth, sourceHeight)

        override fun draw(canvas: Canvas, paint: Paint) {
            canvas.drawRoundRect(
                0f,
                0f,
                canvas.width.toFloat(),
                canvas.height.toFloat(),
                cornerRadiusPx,
                cornerRadiusPx,
                paint
            )
        }
    })

    private fun getSquareShapeMaskDrawableTransformation(
        name: String,
        @DrawableRes
        shapeMaskDrawableId: Int,
        context: Context,
    ) = ShapeMaskImageTransformation(
        SquareDrawableShapeMask(
            name = name,
            shapeMaskDrawableId = shapeMaskDrawableId,
            context = context,
        )
    )

    private fun drawSauce(canvas: Canvas, paint: Paint) {
        val radius = min(canvas.width, canvas.height) / 2f
        canvas.drawRoundRect(
            0f,
            0f,
            canvas.width.toFloat(),
            canvas.height.toFloat(),
            radius,
            radius,
            paint
        )
    }
}
