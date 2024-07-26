package ua.com.radiokot.photoprism.util.images

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.DrawableRes
import com.squareup.picasso.Transformation
import ua.com.radiokot.photoprism.R

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

    fun nona(context: Context) =
        getSquareShapeMaskDrawableTransformation(
            name = "nona",
            shapeMaskDrawableId = R.drawable.image_shape_nona,
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
}
