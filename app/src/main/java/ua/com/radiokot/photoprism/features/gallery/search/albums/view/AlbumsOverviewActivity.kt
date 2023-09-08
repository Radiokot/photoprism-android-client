package ua.com.radiokot.photoprism.features.gallery.search.albums.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityAlbumsOverviewBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.AlbumListItem
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.AlbumsOverviewViewModel
import ua.com.radiokot.photoprism.features.gallery.search.extension.bindToViewModel
import ua.com.radiokot.photoprism.features.gallery.search.extension.fixCloseButtonColor
import ua.com.radiokot.photoprism.features.gallery.search.extension.hideUnderline
import ua.com.radiokot.photoprism.view.ErrorView

class AlbumsOverviewActivity : BaseActivity() {
    private val log = kLogger("AlbumsOverviewActivity")

    private lateinit var view: ActivityAlbumsOverviewBinding
    private val viewModel: AlbumsOverviewViewModel by viewModel()
    private val adapter = ItemAdapter<AlbumListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityAlbumsOverviewBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.selectedAlbumUid.value = intent.getStringExtra(SELECTED_ALBUM_UID_EXTRA)

        // Init the list once it is laid out.
        view.albumsRecyclerView.doOnPreDraw {
            initList()
        }
        initErrorView()
        initSwipeRefresh()

        subscribeToData()
        subscribeToEvents()

        // Allow the view model to intercept back press.
        onBackPressedDispatcher.addCallback(viewModel.backPressedCallback)
    }

    private fun initList() {
        val albumsAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: AlbumListItem, _ ->
                viewModel.onAlbumItemClicked(item)
                true
            }
        }

        with(view.albumsRecyclerView) {
            // Safe dimensions of the list keeping from division by 0.
            // The fallback size is not supposed to be taken,
            // as it means initializing of a not laid out list.
            val listWidth = measuredWidth
                .takeIf { it > 0 }
                ?: FALLBACK_LIST_SIZE
                    .also {
                        log.warn { "initList(): used_fallback_width" }
                    }

            val minItemWidthPx =
                resources.getDimensionPixelSize(R.dimen.list_item_album_width)
            val spanCount = (listWidth / minItemWidthPx).coerceAtLeast(1)

            log.debug {
                "initList(): calculated_grid:" +
                        "\nspanCount=$spanCount," +
                        "\nrowWidth=$listWidth," +
                        "\nminItemWidthPx=$minItemWidthPx"
            }

            adapter = albumsAdapter

            // Add the row spacing and make the items fill the column width
            // by overriding the layout manager layout params factory.
            layoutManager = object : GridLayoutManager(context, spanCount) {
                val rowSpacing: Int =
                    resources.getDimensionPixelSize(R.dimen.list_item_album_margin_end)

                override fun generateLayoutParams(
                    c: Context,
                    attrs: AttributeSet
                ): RecyclerView.LayoutParams {
                    return super.generateLayoutParams(c, attrs).apply {
                        width = RecyclerView.LayoutParams.MATCH_PARENT
                        bottomMargin = rowSpacing
                    }
                }
            }

            FastScrollerBuilder(this)
                .useMd2Style()
                .setTrackDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.fast_scroll_track
                    )!!
                )
                .setThumbDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.fast_scroll_thumb
                    )!!
                )
                .build()
        }
    }

    private fun initErrorView() {
        view.errorView.replaces(view.albumsRecyclerView)
    }

    private fun initSwipeRefresh() {
        view.swipeRefreshLayout.setColorSchemeColors(
            MaterialColors.getColor(
                view.swipeRefreshLayout,
                com.google.android.material.R.attr.colorPrimary,
            )
        )
        view.swipeRefreshLayout.setOnRefreshListener(viewModel::onSwipeRefreshPulled)
    }

    private fun subscribeToData() {
        viewModel.itemsList.observe(this, adapter::setNewList)

        viewModel.isLoading.observe(this, view.swipeRefreshLayout::setRefreshing)

        viewModel.mainError.observe(this) { mainError ->
            when (mainError) {
                AlbumsOverviewViewModel.Error.LoadingFailed ->
                    view.errorView.showError(
                        ErrorView.Error.General(
                            context = view.errorView.context,
                            messageRes = R.string.failed_to_load_albums,
                            retryButtonTextRes = R.string.try_again,
                            retryButtonClickListener = viewModel::onRetryClicked
                        )
                    )

                AlbumsOverviewViewModel.Error.NothingFound ->
                    view.errorView.showError(
                        ErrorView.Error.EmptyView(
                            context = view.errorView.context,
                            messageRes = R.string.no_albums_found,
                        )
                    )

                null ->
                    view.errorView.hide()
            }
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            AlbumsOverviewViewModel.Event.ShowFloatingLoadingFailedError ->
                showFloatingLoadingFailedError()

            is AlbumsOverviewViewModel.Event.FinishWithResult ->
                finishWithResult(event.selectedAlbumUid)

            is AlbumsOverviewViewModel.Event.Finish ->
                finish()
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }.autoDispose(this)

    private fun showFloatingLoadingFailedError() {
        Snackbar.make(
            view.swipeRefreshLayout,
            getString(R.string.failed_to_load_albums),
            Snackbar.LENGTH_SHORT
        )
            .setAction(R.string.try_again) { viewModel.onRetryClicked() }
            .show()
    }

    private fun finishWithResult(selectedAlbumUid: String?) {
        log.debug {
            "finishWithResult(): finishing:" +
                    "\nselectedAlbumUid=$selectedAlbumUid"
        }

        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                createResult(selectedAlbumUid)
            )
        )
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_overview, menu)

        // Set up the search.
        with(menu?.findItem(R.id.search_view)?.actionView as SearchView) {
            queryHint = getString(R.string.enter_the_query)
            fixCloseButtonColor()
            hideUnderline()
            bindToViewModel(viewModel, this@AlbumsOverviewActivity)
        }

        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val FALLBACK_LIST_SIZE = 100
        private const val SELECTED_ALBUM_UID_EXTRA = "selected_album_uid"

        fun getBundle(selectedAlbumUid: String?) = Bundle().apply {
            putString(SELECTED_ALBUM_UID_EXTRA, selectedAlbumUid)
        }

        private fun createResult(selectedAlbumUid: String?) = Bundle().apply {
            putString(SELECTED_ALBUM_UID_EXTRA, selectedAlbumUid)
        }

        fun getSelectedAlbumUid(bundle: Bundle): String? =
            bundle.getString(SELECTED_ALBUM_UID_EXTRA, "")
                .takeIf(String::isNotEmpty)
    }
}
