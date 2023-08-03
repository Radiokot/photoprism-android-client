package ua.com.radiokot.photoprism.features.gallery.view.model

import android.content.Context
import android.os.Parcelable
import android.text.format.Formatter
import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemMediaFileBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

@Parcelize
class MediaFileListItem(
    val name: String,
    val thumbnailUrl: String,
    val size: String,
    val mimeType: String,
    val source: GalleryMedia.File?,
) : AbstractItem<MediaFileListItem.ViewHolder>(), Parcelable {

    constructor(
        source: GalleryMedia.File,
        context: Context,
    ) : this(
        name = source.name,
        thumbnailUrl = source.smallThumbnailUrl,
        mimeType = source.mimeType,
        size = Formatter.formatFileSize(context, source.sizeBytes),
        source = source,
    )

    override val type: Int
        get() = R.id.list_item_media_file

    override val layoutRes: Int
        get() = R.layout.list_item_media_file

    override var identifier: Long
        // The getter must be kept for Parcelable.
        get() = source?.hashCode()?.toLong() ?: hashCode().toLong()
        set(_) = error("Do not overwrite my value!")

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(
        itemView: View,
    ) : FastAdapter.ViewHolder<MediaFileListItem>(itemView), KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = ListItemMediaFileBinding.bind(itemView)
        private val picasso: Picasso by inject()

        override fun bindView(item: MediaFileListItem, payloads: List<Any>) {
            view.nameTextView.text = item.name
            view.nameTextView.isSelected = true

            view.sizeTextView.text = item.size
            view.typeTextView.text = item.mimeType

            picasso
                .load(item.thumbnailUrl)
                .hardwareConfigIfAvailable()
                .placeholder(R.drawable.image_placeholder)
                .fit()
                .centerCrop()
                .into(view.thumbnailImageView)
        }

        override fun unbindView(item: MediaFileListItem) {
            picasso.cancelRequest(view.thumbnailImageView)
        }
    }
}
