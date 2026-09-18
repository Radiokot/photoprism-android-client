package ua.com.radiokot.photoprism.features.viewer.view

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.extension.autoDispose
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
    private var panorama3DView: Panorama3DView? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession() || Build.VERSION.SDK_INT < 23) {
            return
        }

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

                    onPanoramaViewCreated(
                        panorama3DView = panorama3DView,
                        savedInstanceState = savedInstanceState,
                    )
                }
            )
            .autoDispose(this)
    }

    private fun onPanoramaViewCreated(
        panorama3DView: Panorama3DView,
        savedInstanceState: Bundle?,
    ) {
        panorama3DView as View

        log.debug {
            "onPanoramaViewCreated(): panorama_view_created:" +
                    "\npanoramaView=$panorama3DView"
        }

        this.panorama3DView = panorama3DView
        setContentView(panorama3DView)

        panorama3DView.yawDegrees =
            savedInstanceState?.getFloat(YAW_DEGREES_EXTRA)
                ?: intent.getFloatExtra(YAW_DEGREES_EXTRA, 0f)

        if (savedInstanceState?.containsKey(PITCH_DEGREES_EXTRA) == true) {
            panorama3DView.pitchDegrees =
                savedInstanceState.getFloat(PITCH_DEGREES_EXTRA)
        }

        if (savedInstanceState?.containsKey(FOV_DEGREES_EXTRA) == true) {
            panorama3DView.fovDegrees =
                savedInstanceState.getFloat(FOV_DEGREES_EXTRA)
        }

        initPanoramaViewGestures(panorama3DView)
    }

    @Suppress("DEPRECATION")
    private fun initPanoramaViewGestures(
        panorama3DView: Panorama3DView,
    ) {
        panorama3DView as View
        panorama3DView.setOnTouchListener(
            Panorama3DViewGestures(
                view = panorama3DView,
            )
        )

        var isFullScreen = false
        window.decorView.setOnSystemUiVisibilityChangeListener { flags ->
            isFullScreen =
                flags and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        panorama3DView.setOnClickListener {
            if (isFullScreen) {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat(YAW_DEGREES_EXTRA, panorama3DView?.yawDegrees ?: 0f)
        outState.putFloat(PITCH_DEGREES_EXTRA, panorama3DView?.pitchDegrees ?: 0f)
        outState.putFloat(FOV_DEGREES_EXTRA, panorama3DView?.fovDegrees ?: 0f)
    }

    companion object {
        private const val IMAGE_URL_EXTRA = "preview_image_url"
        private const val PROJECTION_EXTRA = "projection"
        private const val YAW_DEGREES_EXTRA = "yaw_degrees"
        private const val PITCH_DEGREES_EXTRA = "pitch_degrees"
        private const val FOV_DEGREES_EXTRA = "fov_degrees"

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
