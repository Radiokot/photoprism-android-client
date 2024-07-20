package ua.com.radiokot.photoprism.features.gallery.search.view

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.text.Editable
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.search.SearchView
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchConfigurationBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.util.ThrottleOnClickListener

/**
 * A view for configuring and applying gallery search.
 */
class GallerySearchView(
    private val viewModel: GallerySearchViewModel,
    private val fragmentManager: FragmentManager,
    private val activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner = activity,
) : LifecycleOwner by lifecycleOwner, KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val log = kLogger("GallerySearchView")

    private val searchFiltersGuideUrl = getKoin()
        .getProperty<String>("searchGuideUrl")
        .checkNotNull { "Missing search filters guide URL" }

    private val context: Context = activity
    private lateinit var searchView: SearchView
    private lateinit var configurationView: GallerySearchConfigurationView
    private val colorOnSurfaceVariant: Int by lazy {
        MaterialColors.getColor(
            searchView,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
    }

    fun init(
        searchView: SearchView,
        configurationView: ViewGallerySearchConfigurationBinding,
    ) {
        this.searchView = searchView

        this.configurationView = GallerySearchConfigurationView(
            view = configurationView,
            viewModel = viewModel,
            activity = activity,
        )

        initSearchView()
        // The configuration view is initialized on showing.

        subscribeToState()
        subscribeToEvents()
    }

    private fun initSearchView() {
        var searchTextStash: Editable?
        searchView.addTransitionListener { _, previousState, newState ->
            log.debug {
                "initSearchView(): search_view_transitioning:" +
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
                        configurationView.initOnce()
                    }
                }

                SearchView.TransitionState.SHOWN -> {
                    // If the view is initialized while the configuration view is already shown,
                    // the configuration view be initialized as well.
                    configurationView.initOnce()
                }

                else -> {
                    // Noting to do.
                }
            }
        }

        with(searchView.findViewById<ImageButton>(com.google.android.material.R.id.search_view_clear_button)) {
            imageTintList = ColorStateList.valueOf(colorOnSurfaceVariant)
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_backspace))
        }

        with(searchView.editText) {
            val searchButtonClickListener = ThrottleOnClickListener {
                viewModel.onSearchClicked()
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    log.debug {
                        "initSearchView(): edit_text_search_key_pressed"
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

        // Fix the back button color. Only this way works.
        (searchView.toolbar as MaterialToolbar).setNavigationIconTint(colorOnSurfaceVariant)

        // Menu.
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

    private fun subscribeToState() {
        viewModel.state.subscribe { state ->
            log.debug {
                "subscribeToState(): received_new_state:" +
                        "\nstate=$state"
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
