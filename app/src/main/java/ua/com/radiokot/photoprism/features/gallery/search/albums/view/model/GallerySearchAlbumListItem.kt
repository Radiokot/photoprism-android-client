package ua.com.radiokot.photoprism.features.gallery.search.albums.view.model

import android.graphics.Color
import android.view.View
import androidx.core.view.ViewCompat
import com.google.android.material.color.MaterialColors
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGallerySearchAlbumBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

data class GallerySearchAlbumListItem(
    val title: String,
    val thumbnailUrl: String,
    val isAlbumSelected: Boolean,
    val source: Album?,
) : AbstractItem<GallerySearchAlbumListItem.ViewHolder>() {
    override val layoutRes: Int =
        R.layout.list_item_gallery_search_album

    override val type: Int =
        R.layout.list_item_gallery_search_album

    override var identifier: Long =
        source?.hashCode()?.toLong() ?: -1L

    constructor(
        source: Album,
        isAlbumSelected: Boolean,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        title = source.title,
        thumbnailUrl = previewUrlFactory.getThumbnailUrl(
            thumbnailHash = source.thumbnailHash,
            sizePx = 500,
        ),
        isAlbumSelected = isAlbumSelected,
        source = source,
    )

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<GallerySearchAlbumListItem>(itemView), KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = ListItemGallerySearchAlbumBinding.bind(itemView)
        private val selectedCardBackgroundColor = MaterialColors.getColor(
            itemView,
            com.google.android.material.R.attr.colorSecondaryContainer,
        )
        private val unselectedCardBackgroundColor = Color.TRANSPARENT
        private val unselectedCardStrokeWidth = view.root.strokeWidth
        private val picasso: Picasso by inject()

        override fun bindView(item: GallerySearchAlbumListItem, payloads: List<Any>) {
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

            with(view.root) {
                setCardBackgroundColor(
                    if (item.isAlbumSelected)
                        selectedCardBackgroundColor
                    else
                        unselectedCardBackgroundColor
                )

                strokeWidth =
                    if (item.isAlbumSelected)
                        0
                    else
                        unselectedCardStrokeWidth

                ViewCompat.setTooltipText(this, item.title)
            }
        }

        override fun unbindView(item: GallerySearchAlbumListItem) {
            picasso.cancelRequest(view.imageView)
        }
    }
}
