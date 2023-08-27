package ua.com.radiokot.photoprism.features.viewer.view.model

import android.util.Size
import android.view.View
import com.github.chrisbanes.photoview.PhotoView
import com.mikepenz.fastadapter.FastAdapter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerImageBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

class ImageViewerPage(
    val previewUrl: String,
    val imageViewSize: Size,
    thumbnailUrl: String,
    source: GalleryMedia?,
) : MediaViewerPage(thumbnailUrl, source) {
    override val type: Int
        get() = R.id.pager_item_media_viewer_image

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer_image

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(
        itemView: View,
    ) : FastAdapter.ViewHolder<ImageViewerPage>(itemView),
        KoinScopeComponent,
        ZoomablePhotoViewHolder {

        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        val view = PagerItemMediaViewerImageBinding.bind(itemView)
        override val photoView: PhotoView
            get() = view.photoView

        private val picasso: Picasso by inject()

        private val imageLoadingCallback = object : Callback {
            override fun onSuccess() {
                view.progressIndicator.hide()
            }

            override fun onError(e: Exception?) {
                view.progressIndicator.hide()
                view.errorTextView.visibility = View.VISIBLE
            }
        }

        override fun bindView(item: ImageViewerPage, payloads: List<Any>) {
            view.progressIndicator.show()
            view.errorTextView.visibility = View.GONE

            picasso
                .load(item.previewUrl)
                .hardwareConfigIfAvailable()
                // Picasso deferred fit is no good when we we want to resize the image
                // considering the zoom factor, so the zoom actually makes sense.
                .resize(item.imageViewSize.width, item.imageViewSize.height)
                .centerInside()
                .onlyScaleDown()
                .into(view.photoView, imageLoadingCallback)
        }

        override fun unbindView(item: ImageViewerPage) {
            picasso.cancelRequest(view.photoView)
        }
    }
}
