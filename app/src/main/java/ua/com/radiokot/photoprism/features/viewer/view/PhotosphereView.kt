package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ua.com.radiokot.photoprism.R

@RequiresApi(Build.VERSION_CODES.M)
class PhotosphereView(
    equirectBitmap: Bitmap,
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs),
    Panorama3DView {

    constructor(
        context: Context,
        attrs: AttributeSet?,
    ) : this(error("This view only be constructed with the bitmap"), context, attrs)

    private val renderer =
        PhotosphereRenderer(
            backgroundColor = ContextCompat.getColor(context, R.color.md_theme_dark_background),
            equirectBitmap = equirectBitmap,
        )

    override var fovDegrees: Float by renderer::fovDegrees

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY

        require(context is LifecycleOwner) {
            "Owner of this view must be a LifecycleOwner"
        }

        context.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                this@PhotosphereView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                this@PhotosphereView.onPause()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                this@PhotosphereView.renderer.close()
            }
        })
    }

    override fun rotateByPointer(
        deltaX: Float,
        deltaY: Float,
    ) =
        renderer.rotateByPointer(deltaX, deltaY)

    override fun rotateByGyro(
        deltaPitch: Float,
        deltaYaw: Float,
        deltaRoll: Float,
    ) =
        renderer.rotateByGyro(deltaPitch, deltaYaw, deltaRoll)

    override fun saveState(
        outState: Bundle,
    ) =
        renderer.saveState(outState)

    override fun restoreState(
        savedInstanceState: Bundle,
    ) =
        renderer.restoreState(savedInstanceState)
}

