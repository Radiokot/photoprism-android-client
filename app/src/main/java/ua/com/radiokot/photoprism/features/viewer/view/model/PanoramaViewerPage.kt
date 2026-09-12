package ua.com.radiokot.photoprism.features.viewer.view.model

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.view.View
import com.panoramagl.PLImage
import com.panoramagl.PLManager
import com.panoramagl.PLRenderer
import com.panoramagl.PLSphericalPanorama
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerPanoramaBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerPageViewHolder

class PanoramaViewerPage(
    val previewUrl: String,
    thumbnailUrl: String,
    source: GalleryMedia?,
) : MediaViewerPage(
    thumbnailUrl,
    source,
) {
    override val type: Int
        get() = R.id.pager_item_media_viewer_panorama

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer_panorama

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(PagerItemMediaViewerPanoramaBinding.bind(v))

    class ViewHolder(
        val view: PagerItemMediaViewerPanoramaBinding,
    ) : MediaViewerPageViewHolder<PanoramaViewerPage>(view.root),
        KoinScopeComponent {

        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val picasso: Picasso by inject()
        private var loadingDisposable: Disposable? = null

        private val panorama = PLSphericalPanorama()
        private val renderer = PLRenderer(
            scene = panorama,
            view = PLManager(view.root.context)
                .also(PLManager::onCreate),
        )

        init {
            view.surfaceView.setRenderer(renderer)
            view.surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        override fun attachToWindow(item: PanoramaViewerPage) {
            super.attachToWindow(item)
            view.progressIndicator.show()

            loadingDisposable?.dispose()
            loadingDisposable =
                picasso
                    .load(item.previewUrl)
                    .config(Bitmap.Config.RGB_565)
                    .intoSingle()
                    .subscribeBy(
                        onError = {
                            view.progressIndicator.hide()
                            onContentPresented()
                            // TODO show the error
                        },
                        onSuccess = { image ->
                            panorama.setImage(
                                PLImage(
                                    bitmap = image,
                                    copy = false,
                                )
                            )
                            panorama.camera.zoomLevel = 2

                            view.progressIndicator.hide()
                            onContentPresented()
                        }
                    )
        }

        override fun unbindView(item: PanoramaViewerPage) {
            loadingDisposable?.dispose()
        }
    }
}
