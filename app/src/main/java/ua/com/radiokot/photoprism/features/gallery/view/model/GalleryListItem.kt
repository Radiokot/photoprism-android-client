package ua.com.radiokot.photoprism.features.gallery.view.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.ShapeAppearanceModel
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
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.util.ItemViewFactory
import ua.com.radiokot.photoprism.util.ItemViewHolderFactory

sealed class GalleryListItem : AbstractItem<ViewHolder>() {
    class Media(
        val thumbnailUrl: String,
        val title: String,
        @DrawableRes
        val mediaTypeIcon: Int?,
        @StringRes
        val mediaTypeName: Int?,
        val isViewButtonVisible: Boolean,
        val isSelectionViewVisible: Boolean,
        val isMediaSelected: Boolean,
        val source: GalleryMedia?,
    ) : GalleryListItem() {
        constructor(
            source: GalleryMedia,
            isViewButtonVisible: Boolean,
            isSelectionViewVisible: Boolean,
            isMediaSelected: Boolean,
        ) : this(
            thumbnailUrl = source.smallThumbnailUrl,
            title = source.title,
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
            isViewButtonVisible = isViewButtonVisible,
            isSelectionViewVisible = isSelectionViewVisible,
            isMediaSelected = isMediaSelected,
            source = source,
        )

        override var identifier: Long =
            source?.hashCode()?.toLong() ?: -1L

        override val type: Int
            get() = R.id.list_item_gallery_media

        override val layoutRes: Int
            get() = R.layout.list_item_gallery_media

        override fun createView(ctx: Context, parent: ViewGroup?): View =
            itemViewFactory.invoke(ctx, parent)

        override fun getViewHolder(v: View): ViewHolder =
            itemViewHolderFactory.invoke(v)

        class ViewHolder(itemView: View) : FastAdapter.ViewHolder<Media>(itemView),
            KoinScopeComponent {
            override val scope: Scope
                get() = getKoin().getScope(DI_SCOPE_SESSION)

            val view = ListItemGalleryMediaBinding.bind(itemView)
            private val selectedImageViewColorFilter: Int =
                ColorUtils.setAlphaComponent(
                    MaterialColors.getColor(
                        view.imageView,
                        com.google.android.material.R.attr.colorPrimary
                    ),
                    100
                )
            private val defaultImageViewShape =
                ShapeAppearanceModel.builder().build()
            private val selectedImageViewShape =
                ShapeAppearanceModel.builder()
                    .setAllCornerSizes(
                        itemView.context.resources
                            .getDimensionPixelSize(R.dimen.selected_gallery_item_corner_radius)
                            .toFloat()
                    )
                    .build()
            private val imageViewScaleAnimationDuration: Long =
                itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime) / 2L
            private val imageViewScaleAnimationInterpolator =
                AccelerateDecelerateInterpolator()
            private val selectedImageViewScale = 0.95f

            private val picasso: Picasso by inject()

            init {
                // Dispatch selection checkbox clicks to the root view
                // and prevent them from changing the checkbox selection state
                // by disabling isClickable – although it is disabled,
                // the click listener is called anyway.
                // isClickable must only be disabled after setting the listener.
                view.selectionCheckBox.setOnClickListener {
                    view.root.callOnClick()
                }
                view.selectionCheckBox.isClickable = false
            }

            override fun bindView(item: Media, payloads: List<Any>) {
                with(view.imageView) {
                    contentDescription = item.title

                    picasso
                        .load(item.thumbnailUrl)
                        .hardwareConfigIfAvailable()
                        .apply {
                            if (isBonded) {
                                noFade()
                                noPlaceholder()
                            } else {
                                placeholder(R.drawable.image_placeholder)
                            }
                        }
                        .fit()
                        .centerCrop()
                        .into(this)

                    isBonded = true
                }

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

                view.viewButton.isVisible = item.isViewButtonVisible
                view.selectionCheckBox.isVisible = item.isSelectionViewVisible

                view.selectionCheckBox.isChecked = item.isMediaSelected

                view.imageView.shapeAppearanceModel =
                    if (item.isMediaSelected)
                        selectedImageViewShape
                    else
                        defaultImageViewShape

                view.root.isSelected = item.isMediaSelected

                if (item.isMediaSelected) {
                    view.imageView.setColorFilter(selectedImageViewColorFilter)
                    if (view.imageView.scaleX != selectedImageViewScale) {
                        if (payloads.contains(PAYLOAD_ANIMATE_SELECTION)) {
                            animateImageScale(selectedImageViewScale)
                        } else {
                            setImageScale(selectedImageViewScale)
                        }
                    }
                    view.root.isSelected = true
                } else {
                    view.imageView.clearColorFilter()
                    if (view.imageView.scaleX != 1f) {
                        if (payloads.contains(PAYLOAD_ANIMATE_SELECTION)) {
                            animateImageScale(1f)
                        } else {
                            setImageScale(1f)
                        }
                    }
                }
            }

            override fun unbindView(item: Media) {
                with(view.imageView) {
                    picasso.cancelRequest(this)
                    setImageDrawable(null)
                    isBonded = false
                }
            }

            private fun animateImageScale(target: Float) {
                view.imageView.clearAnimation()
                view.imageView.animate()
                    .scaleX(target)
                    .scaleY(target)
                    .setDuration(imageViewScaleAnimationDuration)
                    .setInterpolator(imageViewScaleAnimationInterpolator)
                    .setListener(null)
            }

            private fun setImageScale(target: Float) {
                view.imageView.scaleX = target
                view.imageView.scaleY = target
            }

            private var ImageView.isBonded: Boolean
                get() = getTag(R.id.thumbnail_image_view) != null
                set(value) {
                    setTag(
                        R.id.thumbnail_image_view,
                        if (value)
                            true
                        else
                            null
                    )
                }

            companion object {
                const val PAYLOAD_ANIMATE_SELECTION = "animate"
            }
        }

        companion object {
            val itemViewFactory: ItemViewFactory = { ctx: Context, parent: ViewGroup? ->
                LayoutInflater.from(ctx)
                    .inflate(R.layout.list_item_gallery_media, parent, false)
            }
            val itemViewHolderFactory: ItemViewHolderFactory<ViewHolder> = ::ViewHolder
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
