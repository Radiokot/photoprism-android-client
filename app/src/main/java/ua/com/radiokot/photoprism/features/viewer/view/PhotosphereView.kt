package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Build
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

    /**
     * Positive to look up, negative to look down, up to 90 degrees.
     */
    override var pitchDegrees: Float by renderer::pitchDegrees

    /**
     * 0 to look at the image center.
     */
    override var yawDegrees: Float by renderer::yawDegrees

    /**
     * The smaller the field of view, the greater the zoom.
     */
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
}

