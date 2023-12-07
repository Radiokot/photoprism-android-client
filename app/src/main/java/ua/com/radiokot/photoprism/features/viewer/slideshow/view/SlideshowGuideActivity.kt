package ua.com.radiokot.photoprism.features.viewer.slideshow.view

import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivitySlideshowGuideBinding

class SlideshowGuideActivity : BaseActivity() {
    private lateinit var view: ActivitySlideshowGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivitySlideshowGuideBinding.inflate(layoutInflater)
        setContentView(view.root)

        initButtons()
        initFullscreen()
        initLandscapeMode()
    }

    private fun initButtons() {
        view.doneButton.setOnClickListener {
            finish()
        }
    }

    private fun initFullscreen() = with(WindowInsetsControllerCompat(window, window.decorView)) {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun initLandscapeMode() {
        // Put the "swipe" hint at the navigation bar side.
        // For this case RIGHT and LEFT must be used instead of END and START.
        val rotation = display?.rotation
        with(view.swipeTextView) {
            if (rotation == Surface.ROTATION_90) {
                gravity = Gravity.RIGHT
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(context, R.drawable.ic_swipe_left),
                    null,
                )
            } else if (rotation == Surface.ROTATION_270) {
                gravity = Gravity.LEFT
                setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.ic_swipe_right),
                    null,
                    null,
                    null,
                )
            }
        }
    }
}
