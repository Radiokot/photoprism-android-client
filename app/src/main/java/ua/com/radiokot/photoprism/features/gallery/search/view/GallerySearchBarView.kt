package ua.com.radiokot.photoprism.features.gallery.search.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.toSpannable
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.color.MaterialColors
import com.google.android.material.search.SearchBar
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetector
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPersonListItem
import ua.com.radiokot.photoprism.features.gallery.search.view.model.AppliedGallerySearch
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import ua.com.radiokot.photoprism.util.images.CenterVerticalImageSpan
import ua.com.radiokot.photoprism.util.images.ImageTransformations
import ua.com.radiokot.photoprism.util.images.SimpleWrappedDrawable
import ua.com.radiokot.photoprism.util.images.TextViewWrappedDrawableTarget
import kotlin.math.roundToInt

/**
 * A view binding gallery search and [SearchBar].
 */
class GallerySearchBarView(
    private val viewModel: GallerySearchViewModel,
    @MenuRes
    private val menuRes: Int?,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner, KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val log = kLogger("GallerySearchBarView")

    private lateinit var searchBar: SearchBar
    private val tvDetector: TvDetector by inject()
    private val picasso: Picasso by inject()
    private val context: Context
        get() = searchBar.context

    fun init(
        searchBar: SearchBar,
    ) {
        this.searchBar = searchBar

        initBar()

        subscribeToState()
    }

    private fun initBar() = with(searchBar) {
        setHint(
            if (tvDetector.isRunningOnTv)
                R.string.use_mouse_to_search_the_library
            else
                R.string.search_the_library
        )

        textView.ellipsize = TextUtils.TruncateAt.END

        // Override the default bar click listener
        // to make the ViewModel in charge of the state.
        post {
            setThrottleOnClickListener {
                viewModel.onSearchBarClicked()
            }
        }

        // Menu.
        @SuppressLint("RestrictedApi")
        if (menuRes != null) {
            // Important. The external inflater is used to avoid setting SearchBar.menuResId
            // Otherwise, this ding dong tries to animate the menu which makes
            // all the items visible during the animation 🤦🏻‍
            SupportMenuInflater(context).inflate(menuRes, searchBar.menu)
            searchBar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.reset_search ->
                        viewModel.onResetClicked()

                    R.id.add_search_bookmark ->
                        viewModel.onAddBookmarkClicked()

                    R.id.edit_search_bookmark ->
                        viewModel.onEditBookmarkClicked()
                }
                true
            }
        }
    }

    private fun subscribeToState() = viewModel.state.subscribe { state ->
        log.debug {
            "subscribeToState(): received_new_state:" +
                    "\nstate=$state"
        }

        // Override logic of the SearchBar text.
        searchBar.post {
            searchBar.text =
                when (state) {
                    is GallerySearchViewModel.State.Applied ->
                        getSearchBarText(
                            search = state.search,
                            textView = searchBar.textView,
                        )

                    is GallerySearchViewModel.State.Configuring ->
                        if (state.alreadyAppliedSearch != null)
                            getSearchBarText(
                                search = state.alreadyAppliedSearch,
                                textView = searchBar.textView,
                            )
                        else
                            null

                    GallerySearchViewModel.State.NoSearch ->
                        null
                }
        }

        with(searchBar.menu) {
            findItem(R.id.reset_search)?.apply {
                isVisible = state is GallerySearchViewModel.State.Applied
            }

            findItem(R.id.add_search_bookmark)?.apply {
                isVisible = state is GallerySearchViewModel.State.Applied
                        && state.search !is AppliedGallerySearch.Bookmarked
            }

            findItem(R.id.edit_search_bookmark)?.apply {
                isVisible = state is GallerySearchViewModel.State.Applied
                        && state.search is AppliedGallerySearch.Bookmarked
            }
        }

        log.debug {
            "subscribeToState(): handled_new_state:" +
                    "\nstate=$state"
        }
    }.autoDispose(this)

    private fun getSearchBarText(
        search: AppliedGallerySearch,
        textView: TextView
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
                context = context,
                colors = textColors,
                sizePx = iconSize,
            )

        fun SpannableStringBuilder.appendCircleImage(url: String) =
            appendCircleImage(
                url = url,
                sizePx = imageSize,
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
                    textView = searchBar.textView,
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
