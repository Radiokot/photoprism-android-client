package ua.com.radiokot.photoprism.features.gallery.view.model

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
import ua.com.radiokot.photoprism.databinding.ListItemAlbumBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.features.gallery.data.model.Album

data class AlbumListItem(
    val title: String,
    val thumbnailUrl: String,
    val isAlbumSelected: Boolean,
    val source: Album?,
) : AbstractItem<AlbumListItem.ViewHolder>() {
    override val layoutRes: Int =
        R.layout.list_item_album

    override val type: Int =
        R.id.list_item_album

    override var identifier: Long =
        source?.hashCode()?.toLong() ?: -1L

    constructor(
        source: Album,
        isAlbumSelected: Boolean,
    ) : this(
        title = source.title,
        thumbnailUrl = source.smallThumbnailUrl,
        isAlbumSelected = isAlbumSelected,
        source = source,
    )

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<AlbumListItem>(itemView), KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = ListItemAlbumBinding.bind(itemView)
        private val selectedCardBackgroundColor = MaterialColors.getColor(
            itemView,
            com.google.android.material.R.attr.colorSecondaryContainer,
        )
        private val unselectedCardBackgroundColor = Color.TRANSPARENT
        private val unselectedCardStrokeWidth = view.listItemAlbum.strokeWidth
        private val picasso: Picasso by inject()

        override fun bindView(item: AlbumListItem, payloads: List<Any>) {
            view.imageView.contentDescription = item.title

            picasso
                .load(item.thumbnailUrl)
                .placeholder(R.drawable.image_placeholder)
                .fit()
                .centerCrop()
                .into(view.imageView)

            view.titleTextView.text = item.title

            with(view.listItemAlbum) {
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

        override fun unbindView(item: AlbumListItem) {
            picasso.cancelRequest(view.imageView)
        }
    }
}
