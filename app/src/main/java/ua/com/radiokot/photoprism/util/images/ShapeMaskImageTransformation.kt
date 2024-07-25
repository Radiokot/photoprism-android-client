package ua.com.radiokot.photoprism.util.images

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import com.squareup.picasso.Transformation
import kotlin.math.min

class ShapeMaskImageTransformation(
    private val mask: ShapeMask,
) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val squareSize = min(source.width, source.height)

        val sourceShader =
            BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply {
                    // Offset the shader to the centered square.
                    setTranslate(
                        -(source.width - squareSize) / 2f,
                        -(source.height - squareSize) / 2f
                    )
                })
            }
        val sourceShaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = sourceShader
        }

        val output = Bitmap.createBitmap(squareSize, squareSize, source.config)
        val canvas = Canvas(output)

        mask.draw(canvas, squareSize, squareSize, sourceShaderPaint)

        // Recycle the source bitmap, because we already generate a new one
        source.recycle()

        return output
    }

    override fun key(): String =
        "ShapeMask-${mask.name}"

    interface ShapeMask {
        val name: String
        fun draw(canvas: Canvas, width: Int, height: Int, paint: Paint)
    }
}
