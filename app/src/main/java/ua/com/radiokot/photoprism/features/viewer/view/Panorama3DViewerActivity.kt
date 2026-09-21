package ua.com.radiokot.photoprism.features.viewer.view

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
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
import ua.com.radiokot.photoprism.features.viewer.view.model.Panorama3DViewerViewModel

class Panorama3DViewerActivity : BaseActivity() {
    private val log = kLogger("Panorama3DViewerActivity")

    private val picasso: Picasso by inject()
    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }
    private val viewModel: Panorama3DViewerViewModel by viewModel {
        parametersOf(
            Panorama3DViewerViewModel.Parameters(
                initialYawDegrees =
                    intent.getFloatExtra(YAW_DEGREES_EXTRA, 0f),
            )
        )
    }
    private lateinit var view: ActivityPanoramaViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        if (Build.VERSION.SDK_INT < 23) {
            log.error {
                "onCreate(): this screen should not be accessible on API < 23"
            }
            finish()
            return
        }

        view = ActivityPanoramaViewerBinding.inflate(layoutInflater)
        setContentView(view.root)

        initToolbar()
        initPanorama()
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
    }

    @SuppressLint("NewApi")
    @Suppress("DEPRECATION")
    private fun initPanorama() {
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
                                orientation = viewModel.cameraOrientation,
                            )
                    }

                    initPanoramaView(
                        panorama3DView = panorama3DView,
                    )
                }
            )
            .autoDispose(this)
    }

    private fun initPanoramaView(
        panorama3DView: Panorama3DView,
    ) {
        panorama3DView as View

        view.contentLayout.addView(panorama3DView)

        panorama3DView.setOnTouchListener(
            Panorama3DViewGestureDetector(
                view = panorama3DView,
                onScaleGesture = viewModel::onScaleGesture,
                onDragGesture = viewModel::onDragGesture,
            )
        )

        viewModel
            .cameraFovDegrees
            .subscribeBy(onNext = panorama3DView::fovDegrees::set)
            .autoDispose(this)

        initFullScreenToggle(
            panorama3DView = panorama3DView,
        )
    }

    @Suppress("DEPRECATION")
    private fun initFullScreenToggle(
        panorama3DView: Panorama3DView,
    ) {
        window.decorView.setOnSystemUiVisibilityChangeListener { flags ->
            viewModel.onFullScreenToggledBySystem(
                isFullScreen =
                    flags and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN,
            )
        }

        viewModel.isFullScreen.subscribe { isFullScreen ->
            if (!isFullScreen) {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                view.toolbar.fadeIn()
            } else {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                view.toolbar.fadeOut()
            }
        }.autoDispose(this)

        (panorama3DView as View).setOnClickListener {
            viewModel.onPanoramaClicked()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.panorama_viewer, menu)

        menu.findItem(R.id.panorama_view_toggle).apply {
            viewModel.isSensorEnabled.subscribe { isSensorView ->
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
            }.autoDispose(this@Panorama3DViewerActivity)

            setOnMenuItemClickListener {
                viewModel.onSensorToggleClicked()
                true
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onScreenResumed()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onScreenPaused()
    }

    companion object {
        private const val IMAGE_URL_EXTRA = "preview_image_url"
        private const val PROJECTION_EXTRA = "projection"
        private const val YAW_DEGREES_EXTRA = "yaw_degrees"

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
