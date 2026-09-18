package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import kotlin.math.roundToInt

class Panorama2DPreviewView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var projection: GalleryMedia.PanoramaProjection? = null
    private var bitmapRect = Rect()
    private val drawPaint = Paint()
    private val viewRect = Rect()
    private var lastDrawTimeNs = System.currentTimeMillis()
    private var bitmapWindowOffsetX = 0f

    /**
     * 0 when looking at the image center.
     * [Float.NaN] when there's no image.
     */
    val yawDegrees: Float
        get() {
            val bitmap = bitmap
                ?: return Float.NaN
            val projection = projection
                ?: return Float.NaN

            return when (projection) {
                GalleryMedia.PanoramaProjection.Equirect -> {
                    val bitmapCenterX = bitmap.width / 2f
                    val bitmapWindowCenterX = bitmapWindowOffsetX + bitmapRect.width() / 2f
                    val deltaX = bitmapWindowCenterX - bitmapCenterX
                    (deltaX / bitmap.width) * 360f
                }
            }
        }

    fun setPanoramaImage(
        bitmap: Bitmap,
        projection: GalleryMedia.PanoramaProjection,
    ) {
        this.bitmap = bitmap
        this.bitmapWindowOffsetX = bitmap.width / 3f
        this.projection = projection
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        viewRect.right = w
        viewRect.bottom = h
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = bitmap
            ?: return
        val projection = projection
            ?: return

        val nowNs = System.currentTimeMillis()
        var dtMs = nowNs - lastDrawTimeNs
        if (dtMs > 17) {
            dtMs = 17
        }
        lastDrawTimeNs = nowNs

        when (projection) {
            GalleryMedia.PanoramaProjection.Equirect -> {
                drawEquirect(
                    canvas = canvas,
                    bitmap = bitmap,
                    dtMs = dtMs,
                )
            }
        }

        invalidate()
    }

    private fun drawEquirect(
        canvas: Canvas,
        bitmap: Bitmap,
        dtMs: Long,
    ) {
        // Rotate the "camera" clockwise.
        // (drag the view-size window across the bitmap from left to right)

        val bitmapWindowHeight = (bitmap.height * 0.55f).toInt()
        val bitmapWindowWidth = (bitmapWindowHeight * (width.toFloat() / height)).toInt()

        bitmapRect.top = (bitmap.height - bitmapWindowHeight) / 2
        bitmapRect.bottom = bitmapRect.top + bitmapWindowHeight

        //                                               Rotation period MS
        bitmapWindowOffsetX += (bitmap.width.toFloat() / 20000f) * dtMs
        bitmapWindowOffsetX %= bitmap.width

        bitmapRect.left = bitmapWindowOffsetX.roundToInt()
        bitmapRect.right = bitmapRect.left + bitmapWindowWidth

        canvas.drawBitmap(
            bitmap,
            bitmapRect,
            viewRect,
            drawPaint,
        )

        if (bitmapRect.right > bitmap.width) {
            bitmapRect.left = -bitmapWindowWidth + bitmapRect.right - bitmap.width
            bitmapRect.right = bitmapRect.left + bitmapWindowWidth
            canvas.drawBitmap(
                bitmap,
                bitmapRect,
                viewRect,
                drawPaint,
            )
        }
    }
}
