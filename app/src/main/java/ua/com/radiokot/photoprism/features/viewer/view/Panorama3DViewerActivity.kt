package ua.com.radiokot.photoprism.features.viewer.view

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.MutableLiveData
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityPanoramaViewerBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.fadeIn
import ua.com.radiokot.photoprism.extension.fadeOut
import ua.com.radiokot.photoprism.extension.hardwareOr565
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

class Panorama3DViewerActivity : BaseActivity() {
    private val log = kLogger("Panorama3DViewerActivity")

    private val picasso: Picasso by inject()
    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }
    private lateinit var view: ActivityPanoramaViewerBinding
    private var panorama3DView: Panorama3DView? = null
    private val isFullScreen = MutableLiveData(false)
    private val isSensorView = MutableLiveData(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession() || Build.VERSION.SDK_INT < 23) {
            return
        }

        view = ActivityPanoramaViewerBinding.inflate(layoutInflater)
        setContentView(view.root)

        initToolbar()
        initPanorama(savedInstanceState)
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
    }

    @SuppressLint("NewApi")
    @Suppress("DEPRECATION")
    private fun initPanorama(
        savedInstanceState: Bundle?,
    ) {
        val imageUrl =
            requireNotNull(intent.getStringExtra(IMAGE_URL_EXTRA)) {
                "No image URL specified"
            }
        val projection =
            requireNotNull(intent.getSerializableExtra(PROJECTION_EXTRA)) {
                "No projection specified"
            } as GalleryMedia.PanoramaProjection

        picasso
            .load(imageUrl)
            .hardwareOr565()
            .intoSingle()
            .doOnSubscribe {
                log.debug {
                    "onCreate(): loading_image:" +
                            "\nurl=$imageUrl"
                }
            }
            .subscribeBy(
                onError = { error ->
                    // TODO
                    log.error(error) {
                        "onCreate(): failed_loading_image:" +
                                "\nurl=$imageUrl"
                    }
                },
                onSuccess = { bitmap ->
                    log.debug {
                        "onCreate(): creating_panorama_view:" +
                                "\nprojection=$projection"
                    }

                    val panorama3DView: Panorama3DView = when (projection) {
                        GalleryMedia.PanoramaProjection.Equirect ->
                            PhotosphereView(
                                context = this,
                                equirectBitmap = bitmap,
                            )
                    }

                    initPanoramaView(
                        panorama3DView = panorama3DView,
                        savedInstanceState = savedInstanceState,
                    )
                }
            )
            .autoDispose(this)
    }

    private fun initPanoramaView(
        panorama3DView: Panorama3DView,
        savedInstanceState: Bundle?,
    ) {
        panorama3DView as View

        if (savedInstanceState != null) {
            panorama3DView.restoreState(savedInstanceState)
        }
        view.contentLayout.addView(panorama3DView)
        this.panorama3DView = panorama3DView

        initFullScreenToggle(
            panorama3DView = panorama3DView,
            savedInstanceState = savedInstanceState,
        )
        initSensorGestureViewToggle(
            panorama3DView = panorama3DView,
            savedInstanceState = savedInstanceState,
        )
    }

    @Suppress("DEPRECATION")
    private fun initFullScreenToggle(
        panorama3DView: Panorama3DView,
        savedInstanceState: Bundle?,
    ) {
        isFullScreen.value =
            savedInstanceState?.getBoolean(IS_FULL_SCREEN_EXTRA) ?: false

        window.decorView.setOnSystemUiVisibilityChangeListener { flags ->
            isFullScreen.value =
                flags and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        isFullScreen.observe(this) { isFullScreen ->
            if (!isFullScreen) {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                view.toolbar.fadeIn()
            } else {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                view.toolbar.fadeOut()
            }
        }

        (panorama3DView as View).setOnClickListener {
            isFullScreen.value = !isFullScreen.value!!
        }
    }

    private val sensorCameraRotationTracker by lazy {
        GyroscopeRotationTracker(
            context = this,
            onRotation = { deltaPitch, deltaRoll, deltaYaw ->
                panorama3DView?.rotateByGyro(
                    deltaPitch = deltaPitch,
                    deltaRoll = deltaRoll,
                    deltaYaw = deltaYaw,
                )
            },
        )
    }

    private fun initSensorGestureViewToggle(
        panorama3DView: Panorama3DView,
        savedInstanceState: Bundle?,
    ) {
        isSensorView.value =
            savedInstanceState?.getBoolean(IS_SENSOR_VIEW_EXTRA) ?: true

        panorama3DView as View

        panorama3DView.setOnTouchListener(
            Panorama3DViewGestures(
                view = panorama3DView,
            )
        )

        isSensorView.observe(this) { isSensorView ->
            if (isSensorView) {
                sensorCameraRotationTracker.start()
            } else {
                sensorCameraRotationTracker.stop()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSensorView.value!! && !sensorCameraRotationTracker.isRunning) {
            sensorCameraRotationTracker.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (sensorCameraRotationTracker.isRunning) {
            sensorCameraRotationTracker.stop()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.panorama_viewer, menu)
        menu.findItem(R.id.panorama_view_toggle).apply {
            isSensorView.observe(this@Panorama3DViewerActivity) { isSensorView ->
                if (isSensorView) {
                    title = getString(R.string.panorama_view_only_use_gestures)
                    icon = ContextCompat.getDrawable(
                        this@Panorama3DViewerActivity,
                        R.drawable.ic_compass_filled
                    )
                } else {
                    title = getString(R.string.panorama_view_use_phone_movement)
                    icon = ContextCompat.getDrawable(
                        this@Panorama3DViewerActivity,
                        R.drawable.ic_compass
                    )
                }
            }

            setOnMenuItemClickListener {
                isSensorView.value = !isSensorView.value!!
                true
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putBoolean(IS_FULL_SCREEN_EXTRA, isFullScreen.value!!)
            putBoolean(IS_SENSOR_VIEW_EXTRA, isSensorView.value!!)
            panorama3DView?.saveState(this)
        }
    }

    companion object {
        private const val IMAGE_URL_EXTRA = "preview_image_url"
        private const val PROJECTION_EXTRA = "projection"
        private const val YAW_DEGREES_EXTRA = "yaw_degrees"
        private const val IS_SENSOR_VIEW_EXTRA = "is_sensor_view"
        private const val IS_FULL_SCREEN_EXTRA = "is_full_screen"

        /**
         * @param yawDegrees 0 to look at the image center
         */
        fun getBundle(
            imageUrl: String,
            projection: GalleryMedia.PanoramaProjection,
            yawDegrees: Float,
        ): Bundle = Bundle().apply {
            putString(IMAGE_URL_EXTRA, imageUrl)
            putSerializable(PROJECTION_EXTRA, projection)
            putFloat(YAW_DEGREES_EXTRA, yawDegrees)
        }
    }
}
