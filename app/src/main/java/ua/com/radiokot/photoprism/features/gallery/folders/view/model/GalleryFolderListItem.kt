package ua.com.radiokot.photoprism.features.gallery.folders.view.model

import android.view.View
import androidx.core.view.ViewCompat
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryFolderBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album

class GalleryFolderListItem(
    private val title: String,
    private val description: String,
    private val thumbnailUrl: String,
    private val source: Any?,
) : AbstractItem<GalleryFolderListItem.ViewHolder>() {

    constructor(source: Album) : this(
        title = source.title,
        description = "todo",
        thumbnailUrl = source.getThumbnailUrl(500),
        source = source,
    )

    override val layoutRes: Int =
        R.layout.list_item_gallery_folder

    override val type: Int =
        R.layout.list_item_gallery_folder

    override var identifier: Long =
        source?.hashCode()?.toLong() ?: -1L

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<GalleryFolderListItem>(itemView), KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = ListItemGalleryFolderBinding.bind(itemView)
        private val picasso: Picasso by inject()

        override fun bindView(item: GalleryFolderListItem, payloads: List<Any>) {
            view.imageView.contentDescription = item.title

            picasso
                .load(item.thumbnailUrl)
                .hardwareConfigIfAvailable()
                .placeholder(R.drawable.image_placeholder)
                .fit()
                .centerCrop()
                .into(view.imageView)

            view.titleTextView.text = item.title
            view.titleTextView.isSelected = true

            view.descriptionTextView.text = item.description
            view.descriptionTextView.isSelected = true

            ViewCompat.setTooltipText(view.root, item.title)
        }

        override fun unbindView(item: GalleryFolderListItem) {
            picasso.cancelRequest(view.imageView)
        }
    }
}
