package ua.com.radiokot.photoprism.features.albums.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityGalleryAlbumsBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.proxyOkResult
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumListItem
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumsViewModel
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.search.extension.bindToViewModel
import ua.com.radiokot.photoprism.features.gallery.search.extension.fixCloseButtonColor
import ua.com.radiokot.photoprism.features.gallery.search.extension.hideUnderline
import ua.com.radiokot.photoprism.features.gallery.view.GallerySingleRepositoryActivity
import ua.com.radiokot.photoprism.util.LocalDate
import ua.com.radiokot.photoprism.view.ErrorView

class AlbumsActivity : BaseActivity() {

    private val log = kLogger("AlbumsActivity")
    private lateinit var view: ActivityGalleryAlbumsBinding
    private val viewModel: AlbumsViewModel by viewModel()
    private val folderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::proxyOkResult,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityGalleryAlbumsBinding.inflate(layoutInflater)
        setContentView(view.root)

        @Suppress("DEPRECATION")
        viewModel.initOnce(
            albumType = intent.getParcelableExtra(ALBUM_TYPE_EXTRA)!!,
            defaultSearchConfig = intent.getParcelableExtra(DEFAULT_SEARCH_CONFIG_EXTRA)!!,
        )

        initToolbar()
        // Init the list once it is laid out.
        view.albumsRecyclerView.doOnPreDraw {
            initList()
        }
        initErrorView()
        initSwipeRefresh()

        subscribeToEvents()

        // Allow the view model to intercept back press.
        onBackPressedDispatcher.addCallback(viewModel.backPressedCallback)

        supportFragmentManager.setFragmentResultListener(
            AlbumSortDialogFragment.REQUEST_KEY,
            this
        ) { _, result ->
            viewModel.onSortDialogResult(
                newSort = AlbumSortDialogFragment.getResult(result),
            )
        }
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = when (viewModel.albumType) {
            Album.TypeName.FOLDER ->
                getString(R.string.folders)

            Album.TypeName.MONTH ->
                getString(R.string.calendar)

            Album.TypeName.ALBUM ->
                getString(R.string.albums)
        }
    }

    private fun initList() {
        val albumsAdapter = ItemAdapter<AlbumListItem>()

        viewModel.itemsList.observe(this, albumsAdapter::setNewList)

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
                resources.getDimensionPixelSize(R.dimen.list_item_gallery_album_width)
            val spanCount = (listWidth / minItemWidthPx).coerceAtLeast(1)

            log.debug {
                "initList(): calculated_grid:" +
                        "\nspanCount=$spanCount," +
                        "\nrowWidth=$listWidth," +
                        "\nminItemWidthPx=$minItemWidthPx"
            }

            adapter = FastAdapter.with(albumsAdapter).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

                onClickListener = { _, _, item: AlbumListItem, _ ->
                    viewModel.onAlbumItemClicked(item)
                    true
                }

                addEventHook(AlbumListItem.CacheClickEvent(viewModel::onCacheIconClicked))
            }

            // Add the row spacing and make the items fill the column width
            // by overriding the layout manager layout params factory.
            layoutManager = object : GridLayoutManager(context, spanCount) {
                val rowSpacing: Int =
                    resources.getDimensionPixelSize(R.dimen.list_item_gallery_album_margin_end)

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

    private fun initSwipeRefresh() = with(view.swipeRefreshLayout) {
        setOnRefreshListener(viewModel::onSwipeRefreshPulled)
        viewModel.isLoading.observe(this@AlbumsActivity, ::setRefreshing)
    }

    private fun initErrorView() {
        view.errorView.replaces(view.albumsRecyclerView)
        viewModel.mainError.observe(this) { mainError ->
            when (mainError) {
                AlbumsViewModel.Error.LoadingFailed ->
                    view.errorView.showError(
                        ErrorView.Error.General(
                            context = view.errorView.context,
                            messageRes =
                            when (viewModel.albumType) {
                                Album.TypeName.FOLDER ->
                                    R.string.failed_to_load_folders

                                Album.TypeName.ALBUM ->
                                    R.string.failed_to_load_albums

                                Album.TypeName.MONTH ->
                                    R.string.failed_to_load_calendar
                            },
                            retryButtonTextRes = R.string.try_again,
                            retryButtonClickListener = viewModel::onRetryClicked
                        )
                    )

                AlbumsViewModel.Error.NothingFound ->
                    view.errorView.showError(
                        ErrorView.Error.EmptyView(
                            context = view.errorView.context,
                            messageRes =
                            when (viewModel.albumType) {
                                Album.TypeName.FOLDER ->
                                    R.string.no_folders_found

                                Album.TypeName.ALBUM ->
                                    R.string.no_albums_found

                                Album.TypeName.MONTH ->
                                    R.string.nothing_found
                            },
                        )
                    )

                null ->
                    view.errorView.hide()
            }
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            AlbumsViewModel.Event.ShowFloatingLoadingFailedError ->
                showFloatingLoadingFailedError()

            is AlbumsViewModel.Event.Finish ->
                finish()

            is AlbumsViewModel.Event.OpenAlbum ->
                openAlbum(
                    title = event.title,
                    monthTitle = event.monthTitle,
                    albumUid = event.albumUid,
                    repositoryParams = event.repositoryParams,
                )

            is AlbumsViewModel.Event.OpenSortDialog ->
                openSortDialog(
                    currentSort = event.currentSort,
                )
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

    private fun showFloatingLoadingFailedError() {
        Snackbar.make(
            view.swipeRefreshLayout,
            when (viewModel.albumType) {
                Album.TypeName.FOLDER ->
                    R.string.failed_to_load_folders

                Album.TypeName.ALBUM ->
                    R.string.failed_to_load_albums

                Album.TypeName.MONTH ->
                    R.string.failed_to_load_calendar
            },
            Snackbar.LENGTH_SHORT
        )
            .setAction(R.string.try_again) { viewModel.onRetryClicked() }
            .show()
    }

    private fun openAlbum(
        title: String,
        monthTitle: LocalDate?,
        albumUid: String?,
        repositoryParams: SimpleGalleryMediaRepository.Params,
    ) = folderLauncher.launch(
        Intent(this, GallerySingleRepositoryActivity::class.java)
            .setAction(intent.action)
            .putExtras(intent.extras ?: Bundle())
            .putExtras(
                GallerySingleRepositoryActivity.getBundle(
                    title = title,
                    monthTitle = monthTitle,
                    albumUid = albumUid,
                    repositoryParams = repositoryParams,
                )
            )
    )

    private fun openSortDialog(
        currentSort: AlbumSort,
    ) {
        val fragment =
            (supportFragmentManager.findFragmentByTag(AlbumSortDialogFragment.TAG)
                    as? AlbumSortDialogFragment)
                ?: AlbumSortDialogFragment().apply {
                    arguments = AlbumSortDialogFragment.getBundle(
                        sort = currentSort,
                    )
                }

        if (!fragment.isAdded || !fragment.showsDialog) {
            fragment.showNow(supportFragmentManager, AlbumSortDialogFragment.TAG)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.gallery_folders, menu)

        // Set up the search.
        with(menu?.findItem(R.id.search_view)?.actionView as SearchView) {
            queryHint = getString(R.string.enter_the_query)
            fixCloseButtonColor()
            hideUnderline()
            bindToViewModel(viewModel, this@AlbumsActivity)
        }

        menu.findItem(R.id.sort)?.setOnMenuItemClickListener {
            viewModel.onSortClicked()
            true
        }

        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val FALLBACK_LIST_SIZE = 100
        private const val ALBUM_TYPE_EXTRA = "album_type"
        private const val DEFAULT_SEARCH_CONFIG_EXTRA = "default_search_config"

        /**
         * @param defaultSearchConfig [SearchConfig] to be used as a base for opening an album.
         * It could, for example, include limited media types.
         */
        fun getBundle(
            albumType: Album.TypeName,
            defaultSearchConfig: SearchConfig,
        ) = Bundle().apply {
            putParcelable(ALBUM_TYPE_EXTRA, albumType)
            putParcelable(DEFAULT_SEARCH_CONFIG_EXTRA, defaultSearchConfig)
        }
    }
}
