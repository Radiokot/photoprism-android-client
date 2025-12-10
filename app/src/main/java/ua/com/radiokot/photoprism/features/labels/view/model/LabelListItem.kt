package ua.com.radiokot.photoprism.features.labels.view.model

import android.view.View
import androidx.core.view.ViewCompat
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemLabelBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.labels.data.model.Label

class LabelListItem(
    private val name: String,
    private val itemCount: Int,
    private val thumbnailUrl: String,
    val source: Label?,
) : AbstractItem<LabelListItem.ViewHolder>() {

    constructor(
        source: Label,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        name = source.name,
        itemCount = source.itemCount,
        thumbnailUrl = previewUrlFactory.getThumbnailUrl(
            thumbnailHash = source.thumbnailHash,
            sizePx = 500,
        ),
        source = source,
    )

    override val type: Int =
        R.layout.list_item_label

    override val layoutRes: Int =
        R.layout.list_item_label

    override var identifier: Long =
        source?.hashCode()?.toLong() ?: -1L

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(
        itemView: View,
    ) : FastAdapter.ViewHolder<LabelListItem>(itemView),
        KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view: ListItemLabelBinding = ListItemLabelBinding.bind(itemView)
        private val picasso: Picasso by inject()

        override fun bindView(item: LabelListItem, payloads: List<Any>) {
            view.imageView.contentDescription = item.name

            picasso
                .load(item.thumbnailUrl)
                .hardwareConfigIfAvailable()
                .placeholder(R.drawable.image_placeholder)
                .fit()
                .centerCrop()
                .into(view.imageView)

            view.nameTextView.text = item.name

            view.descriptionTextView.text =
                view.root.context.resources.getQuantityString(
                    R.plurals.items,
                    item.itemCount,
                    item.itemCount,
                )

            ViewCompat.setTooltipText(view.root, item.name)
        }

        override fun unbindView(item: LabelListItem) {
            picasso.cancelRequest(view.imageView)
        }
    }
}
