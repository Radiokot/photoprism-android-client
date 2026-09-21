package ua.com.radiokot.photoprism.features.viewer.view

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class Panorama3DViewGestureDetector(
    private val view: View,
    private val onScaleGesture: OnPanorama3DViewScaleGesture,
    private val onDragGesture: OnPanorama3DViewDragGesture,
) : View.OnTouchListener {

    private val scaleDetector = ScaleGestureDetector(
        view.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor

                if (scaleFactor == 0f) {
                    return false
                }

                onScaleGesture(
                    factor = scaleFactor,
                )

                return true
            }
        }
    )

    private val otherDetector = GestureDetector(
        view.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {

                onDragGesture(
                    deltaX = distanceX,
                    deltaY = distanceY,
                )

                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                view.callOnClick()
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

fun interface OnPanorama3DViewScaleGesture {
    operator fun invoke(factor: Float)
}

fun interface OnPanorama3DViewDragGesture {
    operator fun invoke(
        deltaX: Float,
        deltaY: Float,
    )
}
