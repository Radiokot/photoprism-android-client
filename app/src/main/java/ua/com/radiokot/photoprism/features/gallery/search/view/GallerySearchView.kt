package ua.com.radiokot.photoprism.features.gallery.search.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.SupportMenuInflater
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.toSpannable
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchConfigurationBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.*
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.GallerySearchAlbumsView
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetector
import ua.com.radiokot.photoprism.features.gallery.search.people.view.GallerySearchPeopleView
import ua.com.radiokot.photoprism.features.gallery.search.view.model.AppliedGallerySearch
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.*
import ua.com.radiokot.photoprism.features.memories.view.MemoriesDemoActivity
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.util.ThrottleOnClickListener
import ua.com.radiokot.photoprism.util.images.CenterVerticalImageSpan
import ua.com.radiokot.photoprism.util.images.CircleImageTransformation
import ua.com.radiokot.photoprism.util.images.SimpleWrappedDrawable
import ua.com.radiokot.photoprism.util.images.TextViewWrappedDrawableTarget
import kotlin.math.roundToInt

/**
 * View for configuring and applying gallery search.
 */
class GallerySearchView(
    private val viewModel: GallerySearchViewModel,
    private val fragmentManager: FragmentManager,
    @MenuRes
    private val menuRes: Int?,
    private val activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner = activity,
) : LifecycleOwner by lifecycleOwner, KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val log = kLogger("GallerySearchView")

    private val searchFiltersGuideUrl = getKoin()
        .getProperty<String>("searchGuideUrl")
        .checkNotNull { "Missing search filters guide URL" }
    private val picasso: Picasso by inject()
    private val tvDetector: TvDetector by inject()

    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var configurationView: ViewGallerySearchConfigurationBinding
    private val context: Context
        get() = searchBar.context
    private lateinit var bookmarksView: GallerySearchBookmarksView
    private lateinit var peopleView: GallerySearchPeopleView
    private lateinit var albumsView: GallerySearchAlbumsView

    fun init(
        searchBar: SearchBar,
        searchView: SearchView,
        configurationView: ViewGallerySearchConfigurationBinding,
    ) {
        this.searchBar = searchBar
        this.searchView = searchView
        this.configurationView = configurationView

        this.bookmarksView = GallerySearchBookmarksView(
            view = configurationView.bookmarksView,
            viewModel = viewModel,
            lifecycleOwner = this,
        )
        this.peopleView = GallerySearchPeopleView(
            view = configurationView.peopleView,
            viewModel = viewModel.peopleViewModel,
            activity = activity,
            lifecycleOwner = this,
        )
        this.albumsView = GallerySearchAlbumsView(
            view = configurationView.albumsView,
            viewModel = viewModel.albumsViewModel,
            activity = activity,
            lifecycleOwner = this,
        )

        initSearchBarAndView()
        initMenus()
        // Bookmarks, albums and people are initialized on config view showing.

        subscribeToData()
        subscribeToState()
        subscribeToEvents()
    }

    private fun initSearchBarAndView() {
        var searchTextStash: Editable?
        searchView.addTransitionListener { _, previousState, newState ->
            log.debug {
                "initSearchBarAndView(): search_view_transitioning:" +
                        "\nprevState=$previousState," +
                        "\nnewState=$newState"
            }

            when (newState) {
                SearchView.TransitionState.SHOWING -> {
                    // Override logic of the SearchView setting the SearchBar text.
                    searchTextStash = searchView.editText.text
                    searchView.editText.post {
                        searchView.editText.text = searchTextStash
                        searchView.editText.setSelection(searchTextStash?.length ?: 0)
                    }

                    // Slightly delay initialization to ease the transition animation.
                    searchView.post {
                        bookmarksView.initListOnce()
                        albumsView.initOnce()
                        peopleView.initListOnce()
                    }
                }

                SearchView.TransitionState.SHOWN -> {
                    // If the view is initialized while the configuration view is already shown,
                    // albums must be initialized as well.
                    bookmarksView.initListOnce()
                    albumsView.initOnce()
                    peopleView.initListOnce()
                }

                else -> {
                    // Noting to do.
                }
            }
        }

        with(searchView.findViewById<ImageButton>(com.google.android.material.R.id.search_view_clear_button)) {
            imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_backspace))
        }

        with(searchView.editText) {
            val searchButtonClickListener = ThrottleOnClickListener {
                viewModel.onSearchClicked()
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    log.debug {
                        "initSearchBarAndView(): edit_text_search_key_pressed"
                    }

                    searchButtonClickListener.onClick(this)
                }

                false
            }

            bindTextTwoWay(viewModel.userQuery)
        }

        // Override the default back click listener
        // to make the ViewModel in charge of the state.
        searchView.toolbar.setNavigationOnClickListener(ThrottleOnClickListener {
            viewModel.onConfigurationBackClicked()
        })

        with(searchBar) {
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

                // TODO: Just for demo purposes.
                if (BuildConfig.DEBUG) {
                    setOnLongClickListener {
                        context.startActivity(Intent(context, MemoriesDemoActivity::class.java))
                        true
                    }
                }
            }
        }

        configurationView.privateContentSwitch.bindCheckedTwoWay(viewModel.includePrivateContent)
        configurationView.onlyFavoriteSwitch.bindCheckedTwoWay(viewModel.onlyFavorite)

        configurationView.searchButton.setThrottleOnClickListener {
            viewModel.onSearchClicked()
        }

        configurationView.resetButton.setThrottleOnClickListener {
            viewModel.onResetClicked()
        }
    }

    private fun initMenus() {
        // Search bar menu.
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

        // Search view (configuration) menu.
        with(searchView) {
            inflateMenu(R.menu.search)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.search_filters_guide ->
                        viewModel.onSearchFiltersGuideClicked()
                }
                false
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
                        isChecked = selectedTypes?.contains(tag) == true
                        isCheckedIconVisible = isChecked
                        isChipIconVisible = !isChecked
                    }
                }
            }
        }

        viewModel.areSomeTypesUnavailable.observe(
            this,
            configurationView.typesNotAvailableNotice::isVisible::set
        )
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

            when (state) {
                is GallerySearchViewModel.State.Applied -> {
                    closeConfigurationView()
                }

                is GallerySearchViewModel.State.Configuring -> {
                    openConfigurationView()
                }

                GallerySearchViewModel.State.NoSearch -> {
                    closeConfigurationView()
                }
            }

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.autoDispose(this)
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

                is GallerySearchViewModel.Event.OpenSearchFiltersGuide ->
                    openSearchFiltersGuide()
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.autoDispose(this)
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
                    val thumbnailUrl = viewModel.peopleViewModel.getPersonThumbnail(personUid)
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
            .transform(CircleImageTransformation.INSTANCE)
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

    private fun openSearchFiltersGuide() {
        context.startActivity(
            Intent(context, WebViewActivity::class.java).putExtras(
                WebViewActivity.getBundle(
                    url = searchFiltersGuideUrl,
                    titleRes = R.string.how_to_search_the_library,
                    pageFinishedInjectionScripts = setOf(
                        WebViewInjectionScriptFactory.Script.GITHUB_WIKI_IMMERSIVE,
                    ),
                )
            )
        )
    }

    private companion object {
        private const val BOOKMARK_DIALOG_TAG = "bookmark"
    }
}
