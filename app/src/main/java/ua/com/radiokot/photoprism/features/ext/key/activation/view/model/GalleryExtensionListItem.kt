package ua.com.radiokot.photoprism.features.ext.key.activation.view.model

import android.view.View
import androidx.annotation.StringRes
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryExtensionBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.view.GalleryExtensionResources

class GalleryExtensionListItem(
    @StringRes
    val title: Int,
    @StringRes
    val description: Int,
    val bannerUrl: String,
) : AbstractItem<GalleryExtensionListItem.ViewHolder>() {
    override val layoutRes: Int
        get() = R.layout.list_item_gallery_extension

    override val type: Int = layoutRes

    constructor(source: GalleryExtension) : this(
        title = GalleryExtensionResources.getTitle(source),
        description = GalleryExtensionResources.getDescription(source),
        bannerUrl = GalleryExtensionResources.getBannerUrl(source)
    )

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<GalleryExtensionListItem>(itemView),
        KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = ListItemGalleryExtensionBinding.bind(itemView)
        private val picasso: Picasso by inject()

        override fun bindView(item: GalleryExtensionListItem, payloads: List<Any>) {
            view.bannerImageView.contentDescription =
                view.bannerImageView.context.getString(item.title)

            picasso
                .load(item.bannerUrl)
                .hardwareConfigIfAvailable()
                .placeholder(R.drawable.image_placeholder)
                .fit()
                .centerCrop()
                .into(view.bannerImageView)

            view.titleTextView.setText(item.title)
            view.descriptionTextView.setText(item.description)
        }

        override fun unbindView(item: GalleryExtensionListItem) {
            picasso.cancelRequest(view.bannerImageView)
        }
    }
}
