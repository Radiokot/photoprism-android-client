package ua.com.radiokot.photoprism.features.viewer.view

import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.hardwareOr565
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

@RequiresApi(Build.VERSION_CODES.M)
class PanoramaViewerActivity : BaseActivity() {
    private val log = kLogger("PanoramaViewerActivity")

    private val picasso: Picasso by inject()
    private var glSurfaceView: GLSurfaceView? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
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

                    val panoramaView = when (projection) {
                        GalleryMedia.PanoramaProjection.Equirect ->
                            PhotosphereView(
                                context = this,
                                equirectBitmap = bitmap,
                            )
                    }

                    log.debug {
                        "onCreate(): panorama_view_created:" +
                                "\npanoramaView=$panoramaView"
                    }

                    this.glSurfaceView = panoramaView
                    setContentView(panoramaView)
                }
            )
            .autoDispose(this)
    }

    companion object {
        private const val IMAGE_URL_EXTRA = "preview_image_url"
        private const val PROJECTION_EXTRA = "projection"

        fun getBundle(
            imageUrl: String,
            projection: GalleryMedia.PanoramaProjection,
        ): Bundle = Bundle().apply {
            putString(IMAGE_URL_EXTRA, imageUrl)
            putSerializable(PROJECTION_EXTRA, projection)
        }
    }
}
