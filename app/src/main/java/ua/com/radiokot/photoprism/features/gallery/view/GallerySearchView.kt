package ua.com.radiokot.photoprism.features.gallery.view

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.res.ColorStateList
import android.graphics.Rect
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MenuRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.SupportMenuInflater
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.toSpannable
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchContentBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.view.model.AppliedGallerySearch
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.SearchBookmarkItem
import kotlin.math.roundToInt


class GallerySearchView(
    private val viewModel: GallerySearchViewModel,
    private val fragmentManager: FragmentManager,
    @MenuRes
    private val menuRes: Int?,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {
    private val log = kLogger("GallerySearchView")

    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var configurationView: ViewGallerySearchContentBinding

    val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            searchView.hide()
        }
    }

    fun init(
        searchBar: SearchBar,
        searchView: SearchView,
        configurationView: ViewGallerySearchContentBinding,
    ) {
        this.searchBar = searchBar
        this.searchView = searchView
        this.configurationView = configurationView

        initView()

        subscribeToData()
        subscribeToState()
        subscribeToEvents()
    }

    private fun initView() {
        var searchTextStash: Editable?
        searchView.addTransitionListener { _, previousState, newState ->
            log.debug {
                "initView(): search_view_transitioning:" +
                        "\nprevState=$previousState," +
                        "\nnewState=$newState"
            }

            when (newState) {
                SearchView.TransitionState.SHOWING -> {
                    viewModel.onConfigurationViewOpening()
                    searchTextStash = searchView.editText.text

                    // Override logic of the SearchView setting the SearchBar text.
                    searchView.editText.post {
                        searchView.editText.text = searchTextStash
                        searchView.editText.setSelection(searchTextStash?.length ?: 0)
                    }
                }
                SearchView.TransitionState.HIDING -> {
                    viewModel.onConfigurationViewClosing()
                }
                else -> {}
            }

            backPressedCallback.isEnabled = newState in setOf(
                SearchView.TransitionState.SHOWING,
                SearchView.TransitionState.SHOWN
            )
        }

        with(searchView.editText) {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    log.debug {
                        "initView(): edit_text_search_key_pressed"
                    }

                    viewModel.onSearchClicked()
                }

                false
            }

            bindTextTwoWay(viewModel.userQuery)
        }

        configurationView.searchButton.setOnClickListener {
            viewModel.onSearchClicked()
        }

        configurationView.resetButton.setOnClickListener {
            viewModel.onResetClicked()
        }

        searchBar.textView.ellipsize = TextUtils.TruncateAt.END

        @SuppressLint("RestrictedApi")
        if (menuRes != null) {
            // Important. The external inflater is used to avoid setting SearchBar.menuResId
            // Otherwise, this ding dong tries to animate the menu which makes
            // all the items visible during the animation 🤦🏻‍
            SupportMenuInflater(searchBar.context).inflate(menuRes, searchBar.menu)
            with(searchBar.menu) {
                findItem(R.id.reset_search)?.setOnMenuItemClickListener {
                    viewModel.onResetClicked()
                    true
                }
                findItem(R.id.add_search_bookmark)?.setOnMenuItemClickListener {
                    viewModel.onAddBookmarkClicked()
                    true
                }
                findItem(R.id.edit_search_bookmark)?.setOnMenuItemClickListener {
                    viewModel.onEditBookmarkClicked()
                    true
                }
            }
        }

        initBookmarksDrag()
    }

    private fun initBookmarksDrag() {
        val rectsWithPrecedingViews = mutableListOf<Pair<Rect, View?>>()

        with(configurationView.bookmarksChipsLayout) {
            setOnDragListener { _, event ->
                if (event.localState !is Chip) {
                    return@setOnDragListener false
                }

                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        rectsWithPrecedingViews.clear()
                        val layoutLocation = IntArray(2)
                            .also { getLocationOnScreen(it) }
                        configurationView.bookmarksChipsLayout.forEachIndexed { i, view ->
                            val viewRelativeLocation = IntArray(2)
                                .also { location ->
                                    view.getLocationOnScreen(location)
                                    location[0] -= layoutLocation[0]
                                    location[1] -= layoutLocation[1]
                                }

                            val marginLayoutParams = view.layoutParams as MarginLayoutParams
                            val viewRelativeRect = Rect(
                                viewRelativeLocation[0],
                                viewRelativeLocation[1],
                                viewRelativeLocation[0] + view.width
                                        + marginLayoutParams.rightMargin
                                        + marginLayoutParams.leftMargin,
                                viewRelativeLocation[1] + view.height
                                        + marginLayoutParams.topMargin
                                        + marginLayoutParams.bottomMargin
                            )

                            val atIndexRect = Rect(
                                viewRelativeRect.left,
                                viewRelativeRect.top,
                                viewRelativeRect.left + viewRelativeRect.width() / 2,
                                viewRelativeRect.top + viewRelativeRect.height() / 2
                            )
                            val nextToIndexRect =
                                Rect(
                                    atIndexRect.right,
                                    atIndexRect.top,
                                    viewRelativeRect.right,
                                    viewRelativeRect.bottom
                                )
                            if (i == childCount - 1) {
                                nextToIndexRect.right = width
                            }

                            rectsWithPrecedingViews.add(atIndexRect to getChildAt(i - 1))
                            rectsWithPrecedingViews.add(nextToIndexRect to view)
                        }

                        log.debug {
                            "initBookmarksDrag(): marked_indexed_regions:" +
                                    "\nsize=${rectsWithPrecedingViews.size}"
                        }

                        return@setOnDragListener true
                    }
                    DragEvent.ACTION_DROP -> {
                        if (!viewModel.canMoveBookmarks) {
                            return@setOnDragListener false
                        }

                        val matchingRectWithPrecedingView: Pair<Rect, View?>? =
                            rectsWithPrecedingViews.find { (rect, _) ->
                                rect.contains(event.x.toInt(), event.y.toInt())
                            }

                        if (matchingRectWithPrecedingView != null) {
                            val precedingView = matchingRectWithPrecedingView.second
                            val precedingViewIndex = indexOfChild(precedingView)
                            val movedView = event.localState as View

                            // Only initiate movement if dropped to a new position.
                            if (precedingView != movedView
                                && precedingViewIndex != indexOfChild(movedView) - 1
                            ) {
                                log.debug {
                                    "initBookmarksDrag(): dropped_to_new_position:" +
                                            "\nprecedingViewIndex=$precedingViewIndex"
                                }

                                viewModel.onBookmarkChipMoved(
                                    item = movedView.tag as SearchBookmarkItem,
                                    placedAfter = precedingView
                                        ?.tag as? SearchBookmarkItem
                                )
                            }

                            return@setOnDragListener true
                        } else {
                            log.debug {
                                "initBookmarksDrag(): dropped_but_unmatched"
                            }

                            return@setOnDragListener false
                        }
                    }
                }
                return@setOnDragListener false
            }
        }
    }

    private fun subscribeToData() {
        val context = configurationView.mediaTypeChipsLayout.context
        viewModel.isApplyButtonEnabled
            .observe(this, configurationView.searchButton::setEnabled)

        val chipSpacing =
            context.resources.getDimensionPixelSize(R.dimen.gallery_search_chip_spacing)
        val chipContext = ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Widget_Material3_Chip_Filter
        )
        val chipLayoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            context.resources.getDimensionPixelSize(R.dimen.gallery_search_chip_height),
        ).apply {
            setMargins(0, 0, chipSpacing, chipSpacing)
        }
        val chipIconTint = ColorStateList.valueOf(
            MaterialColors.getColor(
                configurationView.mediaTypeChipsLayout,
                com.google.android.material.R.attr.colorOnSurfaceVariant
            )
        )

        with(configurationView.mediaTypeChipsLayout) {
            viewModel.availableMediaTypes.observe(this@GallerySearchView) { availableTypes ->
                availableTypes.forEach { mediaTypeName ->
                    addView(
                        Chip(chipContext).apply {
                            tag = mediaTypeName
                            setText(
                                GalleryMediaTypeResources.getName(
                                    mediaTypeName
                                )
                            )
                            setChipIconResource(
                                GalleryMediaTypeResources.getIcon(
                                    mediaTypeName
                                )
                            )
                            setChipIconTint(chipIconTint)

                            setEnsureMinTouchTargetSize(false)
                            isCheckable = true

                            setOnClickListener {
                                viewModel.onAvailableMediaTypeClicked(mediaTypeName)
                            }
                        },
                        chipLayoutParams,
                    )
                }
            }

            viewModel.selectedMediaTypes.observe(this@GallerySearchView) { selectedTypes ->
                forEach { chip ->
                    with(chip as Chip) {
                        isChecked = selectedTypes.contains(tag)
                        isCheckedIconVisible = isChecked
                        isChipIconVisible = !isChecked
                    }
                }
            }
        }

        val bookmarkChipClickListener = View.OnClickListener { chip ->
            viewModel.onBookmarkChipClicked(chip.tag as SearchBookmarkItem)
        }
        val bookmarkChipEditClickListener = View.OnClickListener { chip ->
            viewModel.onBookmarkChipEditClicked(chip.tag as SearchBookmarkItem)
        }
        val bookmarkChipLongClickListener = View.OnLongClickListener { chip ->
            if (viewModel.canMoveBookmarks) {
                val dragShadow = View.DragShadowBuilder(chip)
                @Suppress("DEPRECATION")
                chip.startDrag(
                    // Setting the clip data allows dropping the bookmark to the query field!
                    // Do not set null
                    ClipData.newPlainText("", (chip.tag as SearchBookmarkItem).dragAndDropContent),
                    dragShadow,
                    chip,
                    0,
                )
            }
            true
        }

        with(configurationView.bookmarksChipsLayout) {
            viewModel.bookmarks.observe(this@GallerySearchView) { bookmarks ->
                removeAllViews()
                bookmarks.forEach { bookmark ->
                    addView(Chip(chipContext).apply {
                        tag = bookmark
                        text = bookmark.name
                        setEnsureMinTouchTargetSize(false)
                        setOnClickListener(bookmarkChipClickListener)

                        isCheckable = false

                        setCloseIconResource(R.drawable.ic_pencil)
                        isCloseIconVisible = true
                        setOnCloseIconClickListener(bookmarkChipEditClickListener)

                        setOnLongClickListener(bookmarkChipLongClickListener)
                    }, chipLayoutParams)
                }
            }
        }

        viewModel.isBookmarksSectionVisible.observe(this) { isBookmarksSectionVisible ->
            configurationView.bookmarksChipsLayout.isVisible = isBookmarksSectionVisible
            configurationView.bookmarksTitleTextView.isVisible = isBookmarksSectionVisible
        }
    }

    private fun subscribeToState() {
        viewModel.state.subscribe { state ->
            log.debug {
                "subscribeToState(): received_new_state:" +
                        "\nstate=$state"
            }

            // Override logic of the SearchBar text.
            searchBar.post {
                searchBar.text =
                    when (state) {
                        is GallerySearchViewModel.State.AppliedSearch ->
                            getSearchBarText(
                                search = state.search,
                                textView = searchBar.textView,
                            )
                        is GallerySearchViewModel.State.ConfiguringSearch ->
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
                    isVisible = state is GallerySearchViewModel.State.AppliedSearch
                }

                findItem(R.id.add_search_bookmark)?.apply {
                    isVisible = state is GallerySearchViewModel.State.AppliedSearch
                            && state.search !is AppliedGallerySearch.Bookmarked
                }

                findItem(R.id.edit_search_bookmark)?.apply {
                    isVisible = state is GallerySearchViewModel.State.AppliedSearch
                            && state.search is AppliedGallerySearch.Bookmarked
                }
            }

            when (state) {
                is GallerySearchViewModel.State.AppliedSearch ->
                    closeConfigurationView()

                is GallerySearchViewModel.State.ConfiguringSearch ->
                    openConfigurationView()

                GallerySearchViewModel.State.NoSearch ->
                    closeConfigurationView()
            }

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.disposeOnDestroy(this)
    }

    private fun subscribeToEvents() {
        viewModel.events.subscribe { event ->
            log.debug {
                "subscribeToEvents(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is GallerySearchViewModel.Event.OpenBookmarkDialog ->
                    openBookmarkDialog(
                        searchConfig = event.searchConfig,
                        existingBookmark = event.existingBookmark,
                    )
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.disposeOnDestroy(this)
    }

    private fun closeConfigurationView() {
        searchView.hide()
    }

    private fun openConfigurationView() {
        searchView.show()
    }

    private fun getSearchBarText(
        search: AppliedGallerySearch,
        textView: TextView
    ): CharSequence {
        if (search is AppliedGallerySearch.Bookmarked) {
            val spannableString = SpannableStringBuilder().apply {
                append(search.bookmark.name)
                setSpan(
                    ForegroundColorSpan(
                        MaterialColors.getColor(
                            textView,
                            com.google.android.material.R.attr.colorPrimary
                        )
                    ),
                    0,
                    length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }

            return spannableString
        }

        val iconPlaceholder = "* "
        val spannableString = SpannableStringBuilder()
            .apply {
                repeat(search.config.mediaTypes.size) {
                    append(iconPlaceholder)
                }

                if (search.config.mediaTypes.isNotEmpty()) {
                    append("  ")
                }
            }
            .append(search.config.userQuery)
            .toSpannable()

        val iconSize = (textView.lineHeight * 0.7).roundToInt()
        val textColors = textView.textColors

        search.config.mediaTypes.forEachIndexed { i, mediaType ->
            val drawable = ContextCompat.getDrawable(
                textView.context,
                GalleryMediaTypeResources.getIcon(mediaType)
            )!!.apply {
                setBounds(0, 0, iconSize, iconSize)
            }
            DrawableCompat.setTintList(drawable, textColors)

            spannableString.setSpan(
                ImageSpan(drawable, ImageSpan.ALIGN_BASELINE),
                i * iconPlaceholder.length,
                (i * iconPlaceholder.length) + 1,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        return spannableString
    }

    private fun openBookmarkDialog(
        searchConfig: SearchConfig,
        existingBookmark: SearchBookmark?
    ) {
        val fragment =
            (fragmentManager.findFragmentByTag(BOOKMARK_DIALOG_TAG) as? SearchBookmarkDialogFragment)
                ?: SearchBookmarkDialogFragment().apply {
                    arguments = SearchBookmarkDialogFragment.getBundle(
                        searchConfig = searchConfig,
                        existingBookmark = existingBookmark,
                    )
                }

        if (!fragment.isAdded || !fragment.showsDialog) {
            fragment.showNow(fragmentManager, BOOKMARK_DIALOG_TAG)
        }
    }

    private companion object {
        private const val BOOKMARK_DIALOG_TAG = "bookmark"
    }
}