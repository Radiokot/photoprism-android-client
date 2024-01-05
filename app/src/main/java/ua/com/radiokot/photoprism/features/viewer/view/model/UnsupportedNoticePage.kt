package ua.com.radiokot.photoprism.features.viewer.view.model

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerUnsupportedBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerPageViewHolder

class UnsupportedNoticePage(
    @StringRes
    val mediaTypeName: Int,
    @DrawableRes
    val mediaTypeIcon: Int?,
    thumbnailUrl: String,
    source: GalleryMedia?,
) : MediaViewerPage(thumbnailUrl, source) {
    override val type: Int
        get() = R.id.pager_item_media_viewer_unsupported

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer_unsupported

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(
        itemView: View,
    ) : MediaViewerPageViewHolder<UnsupportedNoticePage>(itemView),
        KoinScopeComponent {

        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = PagerItemMediaViewerUnsupportedBinding.bind(itemView)
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

        override fun bindView(item: UnsupportedNoticePage, payloads: List<Any>) {
            super.bindView(item, payloads)

            view.progressIndicator.show()
            view.errorTextView.visibility = View.GONE

            with(view.mediaTypeTextView) {
                setText(item.mediaTypeName)
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    item.mediaTypeIcon?.let { ContextCompat.getDrawable(context, it) },
                    null,
                    null,
                    null
                )
            }

            picasso
                .load(item.thumbnailUrl)
                .hardwareConfigIfAvailable()
                .placeholder(R.drawable.image_placeholder)
                .fit()
                .into(view.thumbnailImageView, imageLoadingCallback)
        }

        override fun attachToWindow(item: UnsupportedNoticePage) {
            onContentPresented()
        }

        override fun unbindView(item: UnsupportedNoticePage) {
            picasso.cancelRequest(view.thumbnailImageView)
        }
    }
}
