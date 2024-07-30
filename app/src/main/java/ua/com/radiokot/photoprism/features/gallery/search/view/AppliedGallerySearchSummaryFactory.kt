package ua.com.radiokot.photoprism.features.gallery.search.view

import android.content.Context
import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.toSpannable
import com.google.android.material.color.MaterialColors
import com.squareup.picasso.Picasso
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPersonListItem
import ua.com.radiokot.photoprism.features.gallery.search.view.model.AppliedGallerySearch
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import ua.com.radiokot.photoprism.util.images.CenterVerticalImageSpan
import ua.com.radiokot.photoprism.util.images.ImageTransformations
import ua.com.radiokot.photoprism.util.images.SimpleWrappedDrawable
import ua.com.radiokot.photoprism.util.images.TextViewWrappedDrawableTarget
import kotlin.math.roundToInt

class AppliedGallerySearchSummaryFactory(
    private val picasso: Picasso,
    private val viewModel: GallerySearchViewModel,
) {
    /**
     * @return A fancy summary of the applied search.
     */
    fun getSummary(
        search: AppliedGallerySearch,
        textView: TextView,
    ): CharSequence {
        fun SpannableStringBuilder.appendHighlightedText(content: String) =
            appendHighlightedText(
                content = content,
                view = textView,
            )

        // For bookmarked search show only bookmark name
        if (search is AppliedGallerySearch.Bookmarked) {
            return SpannableStringBuilder().apply {
                appendHighlightedText(search.bookmark.name)
            }
        }

        val iconSize = (textView.lineHeight * 0.8).roundToInt()
        val imageSize = (textView.lineHeight * 1.3).roundToInt()
        val textColors = textView.textColors

        fun SpannableStringBuilder.appendIcon(@DrawableRes id: Int) =
            appendIcon(
                id = id,
                context = textView.context,
                colors = textColors,
                sizePx = iconSize,
            )

        fun SpannableStringBuilder.appendCircleImage(url: String) =
            appendCircleImage(
                url = url,
                sizePx = imageSize,
                textView = textView,
            )

        val spannableString = SpannableStringBuilder()
            .apply {
                search.config.personIds.forEach { personUid ->
                    // If the URL is missing, the placeholder will be shown.
                    val thumbnailUrl = viewModel.peopleViewModel.getPersonThumbnail(
                        uid = personUid,
                        // Use the thumbnail size from the person list item
                        // although it is too big for such a tiny view.
                        // The is most certainly already cached by the people list,
                        // hence can be displayed instantly.
                        viewSizePx = GallerySearchPersonListItem.DEFAULT_THUMBNAIL_SIZE,
                    )
                        ?: "missing:/"
                    appendCircleImage(thumbnailUrl)
                }

                search.config.mediaTypes?.forEach { mediaType ->
                    appendIcon(GalleryMediaTypeResources.getIcon(mediaType))
                }

                if (search.config.includePrivate) {
                    appendIcon(R.drawable.ic_eye_off)
                }

                if (search.config.onlyFavorite) {
                    appendIcon(R.drawable.ic_favorite)
                }

                // If an album is selected, show icon and, if possible, the title.
                val albumUid = search.config.albumUid
                if (albumUid != null) {
                    appendIcon(R.drawable.ic_album)

                    val albumTitle = viewModel.albumsViewModel.getAlbumTitle(albumUid)
                    if (albumTitle != null) {
                        appendHighlightedText(albumTitle)
                        if (search.config.userQuery.isNotEmpty()) {
                            append(" ")
                        }
                    }
                }
            }
            .append(search.config.userQuery)
            .toSpannable()

        return spannableString
    }

    private fun SpannableStringBuilder.appendHighlightedText(content: String, view: TextView) {
        append(content)
        setSpan(
            ForegroundColorSpan(
                MaterialColors.getColor(
                    view,
                    com.google.android.material.R.attr.colorPrimary
                )
            ),
            0,
            length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }

    private fun SpannableStringBuilder.appendIcon(
        @DrawableRes id: Int,
        context: Context,
        sizePx: Int,
        colors: ColorStateList,
        end: String = " "
    ) {
        val drawable = ContextCompat.getDrawable(
            context,
            id
        )!!.apply {
            setBounds(0, 0, sizePx, sizePx)
            DrawableCompat.setTintList(this, colors)
        }
        append("x")
        setSpan(
            CenterVerticalImageSpan(drawable),
            length - 1,
            length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        append(end)
    }

    private fun SpannableStringBuilder.appendCircleImage(
        url: String,
        sizePx: Int,
        textView: TextView,
        end: String = " "
    ) {
        val wrappedDrawable = SimpleWrappedDrawable(
            defaultWidthPx = sizePx,
            defaultHeightPx = sizePx,
        )

        picasso
            .load(url)
            .resize(sizePx, sizePx)
            .centerInside()
            .noFade()
            .transform(ImageTransformations.circle)
            .placeholder(R.drawable.image_placeholder_circle)
            .into(
                TextViewWrappedDrawableTarget(
                    textView = textView,
                    wrappedDrawable = wrappedDrawable,
                )
            )

        append("x")
        setSpan(
            CenterVerticalImageSpan(wrappedDrawable),
            length - 1,
            length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        append(end)
    }
}
