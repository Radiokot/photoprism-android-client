package ua.com.radiokot.photoprism.features.gallery.view.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryMediaBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import kotlin.random.Random

sealed class GalleryListItem : AbstractItem<ViewHolder>() {
    class Media(
        val thumbnailUrl: String,
        val name: String,
        @DrawableRes
        val mediaTypeIcon: Int?,
        @StringRes
        val mediaTypeName: Int?,
        val source: GalleryMedia?
    ) : GalleryListItem() {

        constructor(source: GalleryMedia) : this(
            thumbnailUrl = source.smallThumbnailUrl,
            name = source.name,
            mediaTypeIcon =
            if (source.media !is GalleryMedia.TypeData.Image)
                GalleryMediaTypeResources.getIcon(source.media.typeName)
            else
                null,
            mediaTypeName =
            if (source.media !is GalleryMedia.TypeData.Image)
                GalleryMediaTypeResources.getName(source.media.typeName)
            else
                null,
            source = source,
        )

        override var identifier: Long
            get() = source?.hashCode()?.toLong() ?: hashCode().toLong()
            set(_) = error("Do not overwrite my value!")

        override val type: Int
            get() = R.id.list_item_gallery_media

        override val layoutRes: Int
            get() = R.layout.list_item_gallery_media

        override fun getViewHolder(v: View): ViewHolder =
            ViewHolder(v)

        class ViewHolder(itemView: View) : FastAdapter.ViewHolder<Media>(itemView) {
            private val view = ListItemGalleryMediaBinding.bind(itemView)

            override fun bindView(item: Media, payloads: List<Any>) {
                view.imageView.contentDescription = item.name

                Picasso.get()
                    .load(item.thumbnailUrl)
                    .placeholder(ColorDrawable(Color.LTGRAY))
                    .fit()
                    .centerCrop()
                    .into(view.imageView)

                with(view.mediaTypeImageView) {
                    if (item.mediaTypeIcon != null) {
                        visibility = View.VISIBLE
                        setImageResource(item.mediaTypeIcon)
                    } else {
                        visibility = View.GONE
                    }

                    contentDescription =
                        if (item.mediaTypeName != null)
                            context.getString(item.mediaTypeName)
                        else
                            null
                }
            }

            override fun unbindView(item: Media) {
                Picasso.get().cancelRequest(view.imageView)
            }
        }
    }

    class Header
    private constructor(
        val text: String?,
        @StringRes
        val textRes: Int?,
        override var identifier: Long,
    ) : GalleryListItem() {

        constructor(
            text: String,
            identifier: Long = Random.nextLong()
        ) : this(
            text = text,
            textRes = null,
            identifier = identifier
        )

        constructor(
            @StringRes
            textRes: Int,
            identifier: Long = Random.nextLong()
        ) : this(
            text = null,
            textRes = textRes,
            identifier = identifier
        )

        override val type: Int
            get() = R.id.list_item_gallery_header

        override val layoutRes: Int
            get() = R.layout.list_item_gallery_header

        override fun getViewHolder(v: View): ViewHolder =
            ViewHolder(v)

        class ViewHolder(itemView: View) :
            FastAdapter.ViewHolder<Header>(itemView) {
            private val headerTextView = itemView as TextView

            override fun bindView(item: Header, payloads: List<Any>) {
                headerTextView.text =
                    item.text ?: headerTextView.context.getString(item.textRes.checkNotNull())
            }

            override fun unbindView(item: Header) {
            }
        }
    }
}