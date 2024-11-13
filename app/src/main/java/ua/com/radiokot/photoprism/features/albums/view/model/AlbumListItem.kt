package ua.com.radiokot.photoprism.features.albums.view.model

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemAlbumBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

class AlbumListItem(
    private val title: String,
    private val description: String?,
    private val thumbnailUrl: String,
    val source: Album?,
) : AbstractItem<AlbumListItem.ViewHolder>() {

    constructor(
        source: Album,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        title = source.title,
        description = source.path?.let { "/$it" },
        thumbnailUrl = previewUrlFactory.getThumbnailUrl(
            thumbnailHash = source.thumbnailHash,
            sizePx = 500,
        ),
        source = source,
    )

    override val layoutRes: Int =
        R.layout.list_item_album

    override val type: Int =
        R.layout.list_item_album

    override var identifier: Long =
        source?.hashCode()?.toLong() ?: -1L

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<AlbumListItem>(itemView), KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = ListItemAlbumBinding.bind(itemView)
        private val picasso: Picasso by inject()

        override fun bindView(item: AlbumListItem, payloads: List<Any>) {
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

            view.descriptionTextView.isVisible = item.description != null
            view.descriptionTextView.text = item.description
            view.descriptionTextView.isSelected = true

            ViewCompat.setTooltipText(view.root, item.title)
        }

        override fun unbindView(item: AlbumListItem) {
            picasso.cancelRequest(view.imageView)
        }
    }
}
