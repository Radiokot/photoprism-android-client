package ua.com.radiokot.photoprism.features.gallery.view.model

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Parcelable
import android.text.format.Formatter
import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemMediaFileBinding
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
        thumbnailUrl = source.thumbnailUrlSmall,
        mimeType = source.mimeType,
        size = Formatter.formatFileSize(context, source.sizeBytes),
        source = source,
    )

    override val type: Int
        get() = R.id.list_item_media_file

    override val layoutRes: Int
        get() = R.layout.list_item_media_file

    override var identifier: Long
        get() = source?.hashCode()?.toLong() ?: hashCode().toLong()
        set(_) = error("Do not overwrite my value!")

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<MediaFileListItem>(itemView) {
        private val view = ListItemMediaFileBinding.bind(itemView)

        override fun bindView(item: MediaFileListItem, payloads: List<Any>) {
            view.nameTextView.text = item.name
            view.nameTextView.isSelected = true

            view.sizeTextView.text = item.size
            view.typeTextView.text = item.mimeType

            Picasso.get()
                .load(item.thumbnailUrl)
                .placeholder(ColorDrawable(Color.LTGRAY))
                .fit()
                .centerCrop()
                .into(view.thumbnailImageView)
        }

        override fun unbindView(item: MediaFileListItem) {
            Picasso.get().cancelRequest(view.thumbnailImageView)
        }
    }
}