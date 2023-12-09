package ua.com.radiokot.photoprism.features.viewer.slideshow.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivitySlideshowGuideBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.viewer.slideshow.data.storage.SlideshowPreferences
import java.util.concurrent.TimeUnit

class SlideshowGuideActivity : BaseActivity() {
    private lateinit var view: ActivitySlideshowGuideBinding

    private val slideshowPreferences: SlideshowPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivitySlideshowGuideBinding.inflate(layoutInflater)
        setContentView(view.root)

        initButtons()
        initFullscreen()
        initLandscapeMode()
    }

    private fun initButtons() {
        // Make the done button clickable after a delay
        // to avoid miss-clicks.
        Single.timer(1, TimeUnit.SECONDS)
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                view.doneButton.setThrottleOnClickListener {
                    slideshowPreferences.isGuideAccepted = true
                    finish()
                }
            }
            .autoDispose(this)
    }

    private fun initFullscreen() = with(WindowInsetsControllerCompat(window, window.decorView)) {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }

    @SuppressLint("RtlHardcoded")
    @Suppress("DEPRECATION")
    private fun initLandscapeMode() {
        // Put the "swipe" hint at the navigation bar side.
        // For this case RIGHT and LEFT must be used instead of END and START.
        val rotation = windowManager.defaultDisplay?.rotation
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
