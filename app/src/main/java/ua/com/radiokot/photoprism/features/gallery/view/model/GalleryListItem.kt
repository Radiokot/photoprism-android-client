package ua.com.radiokot.photoprism.features.gallery.view.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryMediaBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.di.UTC_DAY_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_DAY_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.extension.animateScale
import ua.com.radiokot.photoprism.extension.capitalized
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.view.GalleryListItemDiffCallback
import ua.com.radiokot.photoprism.util.ItemViewFactory
import ua.com.radiokot.photoprism.util.ItemViewHolderFactory
import ua.com.radiokot.photoprism.util.LocalDate
import java.text.DateFormat

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
        val isFavorite: Boolean,
        val source: GalleryMedia?,
        /**
         * Do not forget to update [GalleryListItemDiffCallback]
         * when changing fields.
         */
    ) : GalleryListItem() {
        constructor(
            source: GalleryMedia,
            isViewButtonVisible: Boolean,
            isSelectionViewVisible: Boolean,
            isMediaSelected: Boolean,
            itemScale: GalleryItemScale,
        ) : this(
            thumbnailUrl = when (itemScale) {
                GalleryItemScale.TINY ->
                    source.getThumbnailUrl(100)

                GalleryItemScale.SMALL,
                GalleryItemScale.NORMAL ->
                    source.getThumbnailUrl(250)

                GalleryItemScale.LARGE,
                GalleryItemScale.HUGE ->
                    source.getThumbnailUrl(500)
            },
            title = source.title,
            mediaTypeIcon =
            // Type icon is visible if it is not an image, unless the scale is tiny.
            if (source.media !is GalleryMedia.TypeData.Image && itemScale != GalleryItemScale.TINY)
                GalleryMediaTypeResources.getIcon(source.media.typeName)
            else
                null,
            mediaTypeName =
            if (source.media !is GalleryMedia.TypeData.Image)
                GalleryMediaTypeResources.getName(source.media.typeName)
            else
                null,
            // View button is visible when needed, unless the scale is tiny.
            isViewButtonVisible = isViewButtonVisible && itemScale != GalleryItemScale.TINY,
            isSelectionViewVisible = isSelectionViewVisible,
            isMediaSelected = isMediaSelected,
            // Favorite icon is visible when needed, unless the scale is tiny.
            isFavorite = source.isFavorite && itemScale != GalleryItemScale.TINY,
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
            private val imageViewScaleAnimationDuration: Int =
                itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime) / 2
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

                view.favoriteImageView.isVisible = item.isFavorite
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
                view.imageView.animateScale(
                    target = target,
                    duration = imageViewScaleAnimationDuration,
                )
            }

            private fun setImageScale(target: Float) {
                view.imageView.scaleX = target
                view.imageView.scaleY = target
            }

            private var ImageView.isBonded: Boolean
                get() = getTag(R.id.thumbnail_image_view) == true
                set(value) {
                    setTag(R.id.thumbnail_image_view, value)
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
        private val value: Value,
        @LayoutRes
        override val layoutRes: Int,
    ) : GalleryListItem() {
        override val type: Int = layoutRes
        override var identifier: Long = value.identifier

        override fun getViewHolder(v: View): ViewHolder =
            ViewHolder(v)

        private sealed interface Value {
            val identifier: Long

            object Today : Value {
                override val identifier: Long = 10647
            }

            sealed class Date(val localDate: LocalDate) : Value {
                class Day(
                    localDate: LocalDate,
                    val withYear: Boolean,
                ) : Date(localDate) {
                    override val identifier: Long = localDate.time
                }

                class Month(
                    localDate: LocalDate,
                    val withYear: Boolean,
                ) : Date(localDate) {
                    // It is very unlikely to get a collision here, as such dates
                    // have no milliseconds.
                    // Although, even if a collision happens, the header simply gets invisible.
                    override val identifier: Long = localDate.time + 1
                }
            }
        }

        class ViewHolder(itemView: View) :
            FastAdapter.ViewHolder<Header>(itemView),
            KoinComponent {
            private val dayDateFormat: DateFormat by inject(named(UTC_DAY_DATE_FORMAT))
            private val dayYearDateFormat: DateFormat by inject(named(UTC_DAY_YEAR_DATE_FORMAT))
            private val monthDateFormat: DateFormat by inject(named(UTC_MONTH_DATE_FORMAT))
            private val monthYearDateFormat: DateFormat by inject(named(UTC_MONTH_YEAR_DATE_FORMAT))

            private val headerTextView = itemView as TextView

            override fun bindView(item: Header, payloads: List<Any>) {
                headerTextView.text = when (val value = item.value) {
                    Value.Today ->
                        itemView.resources.getString(R.string.today)

                    is Value.Date.Day ->
                        if (value.withYear)
                            dayYearDateFormat.format(value.localDate).capitalized()
                        else
                            dayDateFormat.format(value.localDate).capitalized()

                    is Value.Date.Month ->
                        if (value.withYear)
                            monthYearDateFormat.format(value.localDate).capitalized()
                        else
                            monthDateFormat.format(value.localDate).capitalized()
                }
            }

            override fun unbindView(item: Header) {
                // No special handling is needed.
            }
        }

        companion object {
            fun today() = Header(
                value = Value.Today,
                layoutRes = R.layout.list_item_gallery_small_header,
            )

            fun day(
                localDate: LocalDate,
                withYear: Boolean,
            ) = Header(
                value = Value.Date.Day(
                    localDate = localDate,
                    withYear = withYear,
                ),
                layoutRes = R.layout.list_item_gallery_small_header,
            )

            fun month(
                localDate: LocalDate,
                withYear: Boolean,
            ) = Header(
                value = Value.Date.Month(
                    localDate = localDate,
                    withYear = withYear,
                ),
                layoutRes = R.layout.list_item_gallery_large_header,
            )
        }
    }
}
