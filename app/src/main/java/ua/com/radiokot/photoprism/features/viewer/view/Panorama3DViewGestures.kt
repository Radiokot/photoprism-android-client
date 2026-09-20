package ua.com.radiokot.photoprism.features.viewer.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class Panorama3DViewGestures(
    private val view: Panorama3DView,
) : View.OnTouchListener {

    private val dragSensitivity = 0.004f
    private val minFov = 30f
    private val maxFov = 90f

    private val context: Context =
        (view as View).context

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor

                if (scaleFactor == 0f) {
                    return false
                }

                view.fovDegrees =
                    (view.fovDegrees / scaleFactor).coerceIn(minFov, maxFov)

                return true
            }
        }
    )

    private val otherDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                val factor = dragSensitivity * view.fovDegrees / maxFov

                view.rotateByPointer(
                    deltaY = distanceY * factor,
                    deltaX = distanceX * factor,
                )

                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                (view as View).callOnClick()
                return super.onSingleTapConfirmed(e)
            }
        }
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {

        var isHandled = scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) {
            isHandled = otherDetector.onTouchEvent(event) || isHandled
        }

        return isHandled
    }
}
