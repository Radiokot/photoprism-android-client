package ua.com.radiokot.photoprism.features.viewer.view.model

import android.view.View
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerPanoramaBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareOr565
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerPageViewHolder

class Panorama2DPreviewViewerPage(
    val previewUrl: String,
    val projection: GalleryMedia.PanoramaProjection,
    val needsOpenPanoramaButton: Boolean,
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
    ) : MediaViewerPageViewHolder<Panorama2DPreviewViewerPage>(view.root),
        KoinScopeComponent {

        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val picasso: Picasso by inject()
        private var isLoadingFinished = false
        private var loadingDisposable: Disposable? = null

        override fun bindView(item: Panorama2DPreviewViewerPage, payloads: List<Any>) {
            super.bindView(item, payloads)

            view.progressIndicator.show()
            view.errorTextView.visibility = View.GONE
            view.openPanoramaButton.visibility = View.GONE
            isLoadingFinished = false

            loadingDisposable?.dispose()
            loadingDisposable =
                picasso
                    .load(item.previewUrl)
                    .hardwareOr565()
                    .intoSingle()
                    .subscribeBy(
                        onError = {
                            view.progressIndicator.hide()
                            view.errorTextView.visibility = View.VISIBLE
                            isLoadingFinished = true
                            onContentPresented()
                        },
                        onSuccess = { bitmap ->
                            view.progressIndicator.hide()
                            view.openPanoramaButton.isVisible = item.needsOpenPanoramaButton

                            isLoadingFinished = true
                            onContentPresented()

                            view.panoramaPreviewView.setPanoramaImage(
                                bitmap = bitmap,
                                projection = item.projection,
                            )
                        }
                    )
        }

        override fun attachToWindow(item: Panorama2DPreviewViewerPage) {
            // If attached without re-binding (swipe to a previous page)
            // and the loading is finished, call the content presentation callback.
            if (isLoadingFinished) {
                onContentPresented()
            }
        }

        override fun unbindView(item: Panorama2DPreviewViewerPage) {
            loadingDisposable?.dispose()
        }
    }
}
