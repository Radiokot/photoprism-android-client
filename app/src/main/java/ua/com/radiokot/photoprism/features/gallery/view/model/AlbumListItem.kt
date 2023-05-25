package ua.com.radiokot.photoprism.features.gallery.view.model

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.gallery.data.model.Album
import ua.com.radiokot.photoprism.util.AsyncListItemViewCache
import ua.com.radiokot.photoprism.util.ListItemViewFactory

data class AlbumListItem(
    val title: String,
    val thumbnailUrl: String,
    val isAlbumSelected: Boolean,
    val source: Album?,
) : AbstractItem<AlbumListItem.ViewHolder>() {
    var viewCache: AsyncListItemViewCache? = null

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

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return viewCache
            .checkNotNull { "The cache must be set up at this moment" }
            .getView(ctx, parent)
    }

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(taggedView: View) :
        FastAdapter.ViewHolder<AlbumListItem>(taggedView), KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val viewAttributes = taggedView.tag as ViewAttributes
        val view = viewAttributes.viewBinding
        private val picasso: Picasso by inject()

        override fun bindView(item: AlbumListItem, payloads: List<Any>) {
            view.imageView.contentDescription = item.title

            picasso
                .load(item.thumbnailUrl)
                .placeholder(ColorDrawable(Color.LTGRAY))
                .fit()
                .centerCrop()
                .into(view.imageView)

            view.titleTextView.text = item.title

            with(view.listItemAlbum) {
                backgroundTintList =
                    if (item.isAlbumSelected)
                        viewAttributes.selectedCardBackgroundTint
                    else
                        null

                strokeWidth =
                    if (item.isAlbumSelected)
                        0
                    else
                        viewAttributes.unselectedCardStrokeWidth
            }
        }

        override fun unbindView(item: AlbumListItem) {
            picasso.cancelRequest(view.imageView)
        }

        class ViewAttributes(
            view: View,
        ) {
            val viewBinding = ListItemAlbumBinding.bind(view)
            val selectedCardBackgroundTint = ColorStateList.valueOf(
                MaterialColors.getColor(
                    viewBinding.listItemAlbum,
                    com.google.android.material.R.attr.colorSecondaryContainer,
                )
            )
            val unselectedCardStrokeWidth = viewBinding.listItemAlbum.strokeWidth
        }
    }

    companion object {
        val viewFactory: ListItemViewFactory = { context, parent ->
            LayoutInflater.from(context)
                .inflate(R.layout.list_item_album, parent, false)
                .apply {
                    tag = ViewHolder.ViewAttributes(this)
                }
        }
    }
}