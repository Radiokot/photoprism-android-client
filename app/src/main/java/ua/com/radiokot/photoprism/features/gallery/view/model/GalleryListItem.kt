package ua.com.radiokot.photoprism.features.gallery.view.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryMediaBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

sealed class GalleryListItem : AbstractItem<ViewHolder>() {
    class Media(
        val thumbnailUrl: String,
        val name: String,
        @DrawableRes
        val mediaTypeIcon: Int?,
        @StringRes
        val mediaTypeName: Int?,
        val source: GalleryMedia?,
    ) : GalleryListItem() {

        constructor(
            source: GalleryMedia,
        ) : this(
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

        class ViewHolder(
            itemView: View,
        ) : FastAdapter.ViewHolder<Media>(itemView), KoinScopeComponent {
            override val scope: Scope
                get() = getKoin().getScope(DI_SCOPE_SESSION)

            private val view = ListItemGalleryMediaBinding.bind(itemView)
            private val picasso: Picasso by inject()

            override fun bindView(item: Media, payloads: List<Any>) {
                view.imageView.contentDescription = item.name

                picasso
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
                picasso!!.cancelRequest(view.imageView)
            }
        }
    }

    class Header
    private constructor(
        val text: String?,
        @StringRes
        val textRes: Int?,
        override var identifier: Long,
        @IdRes
        override val type: Int,
        @LayoutRes
        override val layoutRes: Int,
    ) : GalleryListItem() {

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

        companion object {
            fun day(text: String) = Header(
                text = text,
                textRes = null,
                identifier = text.hashCode().toLong(),
                type = R.id.list_item_gallery_day_header,
                layoutRes = R.layout.list_item_gallery_day_header,
            )

            fun day(@StringRes textRes: Int) = Header(
                text = null,
                textRes = textRes,
                identifier = textRes.toLong(),
                type = R.id.list_item_gallery_day_header,
                layoutRes = R.layout.list_item_gallery_day_header,
            )

            fun month(text: String) = Header(
                text = text,
                textRes = null,
                identifier = text.hashCode().toLong(),
                type = R.id.list_item_month_header,
                layoutRes = R.layout.list_item_gallery_month_header,
            )

            fun month(@StringRes textRes: Int) = Header(
                text = null,
                textRes = textRes,
                identifier = textRes.toLong(),
                type = R.id.list_item_month_header,
                layoutRes = R.layout.list_item_gallery_month_header,
            )
        }
    }
}