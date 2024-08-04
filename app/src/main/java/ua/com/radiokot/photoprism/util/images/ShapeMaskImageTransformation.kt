package ua.com.radiokot.photoprism.util.images

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import com.squareup.picasso.Transformation
import kotlin.math.min

class ShapeMaskImageTransformation(
    private val shapeMask: ShapeMask,
) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val shapeMaskRect = shapeMask.getRect(
            sourceWidth = source.width,
            sourceHeight = source.height,
        )

        val sourceShaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply {
                    // Offset the shader to start drawing from the beginning
                    // of the shape mask rect.
                    setTranslate(
                        -shapeMaskRect.left.toFloat(),
                        -shapeMaskRect.top.toFloat(),
                    )
                })
            }
            isAntiAlias = true
        }

        val resultBitmap = Bitmap.createBitmap(
            shapeMaskRect.width(),
            shapeMaskRect.height(),
            source.config
        )
        val resultCanvas = Canvas(resultBitmap)

        shapeMask.draw(resultCanvas, sourceShaderPaint)

        // Recycle the source bitmap, we have the new one.
        source.recycle()

        return resultBitmap
    }

    override fun key(): String =
        "ShapeMask-${shapeMask.name}"

    interface ShapeMask {
        /**
         * A unique name of the shape.
         */
        val name: String

        /**
         * @return a [Rect] within the source bitmap to crop the result to.
         */
        fun getRect(sourceWidth: Int, sourceHeight: Int): Rect

        /**
         * Draws the mask on the prepared [canvas] with given [paint] with a shader.
         * The canvas matches the [Rect] returned from [getRect].
         */
        fun draw(canvas: Canvas, paint: Paint)

        companion object {
            fun getCenterSquareRect(sourceWidth: Int, sourceHeight: Int): Rect {
                val squareSize = min(sourceWidth, sourceHeight)
                val horizontalMargin = (sourceWidth - squareSize) / 2
                val verticalMargin = (sourceHeight - squareSize) / 2
                return Rect(
                    horizontalMargin,
                    verticalMargin,
                    horizontalMargin + squareSize,
                    verticalMargin + squareSize
                )
            }
        }
    }
}
