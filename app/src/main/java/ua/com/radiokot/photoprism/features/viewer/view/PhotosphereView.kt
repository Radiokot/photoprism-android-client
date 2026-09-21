package ua.com.radiokot.photoprism.features.viewer.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ua.com.radiokot.photoprism.features.viewer.view.model.Quaternion

/**
 * Renders equirect bitmap as a photosphere.
 * Camera [orientation] is hoisted out of the view
 * so it's not lost on activity re-creation
 * and keeps receiving gyro updates during this process.
 */
@SuppressLint("ViewConstructor")
@RequiresApi(Build.VERSION_CODES.M)
class PhotosphereView(
    orientation: Quaternion,
    equirectBitmap: Bitmap,
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs),
    Panorama3DView {

    private val renderer =
        PhotosphereRenderer(
            backgroundColor = Color.BLACK,
            equirectBitmap = equirectBitmap,
            orientation = orientation,
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
}

