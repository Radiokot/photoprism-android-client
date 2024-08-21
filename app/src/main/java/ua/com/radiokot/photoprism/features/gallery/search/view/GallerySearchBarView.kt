package ua.com.radiokot.photoprism.features.gallery.search.view

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.lifecycle.LifecycleOwner
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
import ua.com.radiokot.photoprism.features.gallery.search.view.model.AppliedGallerySearch
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel

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
    private lateinit var searchSummaryFactory: AppliedGallerySearchSummaryFactory

    fun init(
        searchBar: SearchBar,
    ) {
        this.searchBar = searchBar
        this.searchSummaryFactory = AppliedGallerySearchSummaryFactory(
            picasso = picasso,
            viewModel = viewModel,
        )

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
                viewModel.onSearchSummaryClicked()
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
                        searchSummaryFactory.getSummary(
                            search = state.search,
                            textView = searchBar.textView,
                        )

                    is GallerySearchViewModel.State.Configuring ->
                        if (state.alreadyAppliedSearch != null)
                            searchSummaryFactory.getSummary(
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
}
