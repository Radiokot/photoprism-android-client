package ua.com.radiokot.photoprism.features.gallery.folders.view

import android.os.Bundle
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.listeners.addClickListener
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityGalleryFolderBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setBetter
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.folders.view.model.GalleryFolderViewModel
import ua.com.radiokot.photoprism.features.gallery.view.GalleryListItemDiffCallback
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryLoadingFooterListItem
import ua.com.radiokot.photoprism.util.AsyncRecycledViewPoolInitializer
import ua.com.radiokot.photoprism.view.ErrorView
import kotlin.math.ceil
import kotlin.math.roundToInt

class GalleryFolderActivity : BaseActivity() {

    private val log = kLogger("GalleryFolderActivity")
    private lateinit var view: ActivityGalleryFolderBinding
    private val viewModel: GalleryFolderViewModel by viewModel()
    private val galleryItemsAdapter = ItemAdapter<GalleryListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityGalleryFolderBinding.inflate(layoutInflater)
        setContentView(view.root)

        @Suppress("DEPRECATION")
        viewModel.initViewingOnce(
            repositoryParams = requireNotNull(intent.getParcelableExtra(REPO_PARAMS_EXTRA)) {
                "No repository params specified"
            },
        )

        // Init the list once it is laid out.
        view.galleryRecyclerView.doOnPreDraw {
            initList(savedInstanceState)
        }
        initToolbar()
        initSwipeRefresh()
        initErrorView()
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent.getStringExtra(TITLE_EXTRA)?.also(::setTitle)
    }

    private fun initSwipeRefresh() = with(view.swipeRefreshLayout) {
        setOnRefreshListener(viewModel::onSwipeRefreshPulled)

        viewModel.isLoading.observe(this@GalleryFolderActivity) { isLoading ->
            // Do not show refreshing if there are no gallery items,
            // as in this case the loading footer is on top and visible.
            // It also must not be shown if the recycler is not on the top,
            // for example, when loading subsequent pages.
            isRefreshing = isLoading
                    && galleryItemsAdapter.adapterItemCount > 0
                    && !view.galleryRecyclerView.canScrollVertically(-1)
        }
    }

    private fun initErrorView() {
        view.errorView.replaces(view.galleryRecyclerView)
        viewModel.mainError.observe(this) { mainError ->
            when (mainError) {
                is GalleryFolderViewModel.Error.LoadingFailed ->
                    view.errorView.showError(
                        ErrorView.Error.General(
                            message = mainError.localizedMessage,
                            retryButtonText = getString(R.string.try_again),
                            retryButtonClickListener = viewModel::onMainErrorRetryClicked
                        )
                    )

                GalleryFolderViewModel.Error.NoMediaFound ->
                    view.errorView.showError(
                        ErrorView.Error.EmptyView(
                            context = view.errorView.context,
                            messageRes = R.string.no_media_found,
                        )
                    )

                null ->
                    view.errorView.hide()
            }
        }
    }

    private fun initList(savedInstanceState: Bundle?) {
        val galleryProgressFooterAdapter = ItemAdapter<GalleryLoadingFooterListItem>().apply {
            setNewList(listOf(GalleryLoadingFooterListItem(isLoading = false, canLoadMore = false)))

            viewModel.isLoading.observe(this@GalleryFolderActivity) { isLoading ->
                this[0] = GalleryLoadingFooterListItem(
                    isLoading = isLoading,
                    canLoadMore = viewModel.canLoadMore
                )
            }
        }

        val galleryAdapter = FastAdapter.with(
            listOf(
                galleryItemsAdapter,
                galleryProgressFooterAdapter
            )
        ).apply {
            stateRestorationPolicy = Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            addClickListener(
                resolveView = { null },
                resolveViews = { viewHolder: ViewHolder ->
                    when (viewHolder) {
                        is GalleryLoadingFooterListItem.ViewHolder ->
                            listOf(viewHolder.view.loadMoreButton)

                        is GalleryListItem.Media.ViewHolder ->
                            listOf(viewHolder.itemView, viewHolder.view.viewButton)

                        else ->
                            listOf(viewHolder.itemView)
                    }
                },
                onClick = { view, _, _, item ->
                    when (item) {
                        is GalleryListItem ->
                            when (view.id) {
                                // TODO item click
                            }

                        is GalleryLoadingFooterListItem ->
                            viewModel.onLoadingFooterLoadMoreClicked()
                    }
                }
            )
        }

        val itemScale = viewModel.itemScale
        val minItemWidthPx =
            resources.getDimensionPixelSize(R.dimen.list_item_gallery_media_min_size)
        val scaledMinItemWidthPx = ceil(minItemWidthPx * itemScale.factor)
            .toInt()
            .coerceAtLeast(1)

        with(view.galleryRecyclerView) {
            // Safe dimensions of the list keeping from division by 0.
            // The fallback size is not supposed to be taken,
            // as it means initializing of a not laid out list.
            val listWidth = measuredWidth
                .takeIf { it > 0 }
                ?: FALLBACK_LIST_SIZE
                    .also {
                        log.warn { "initList(): used_fallback_width" }
                    }
            val listHeight = measuredHeight
                .takeIf { it > 0 }
                ?: FALLBACK_LIST_SIZE
                    .also {
                        log.warn { "initList(): used_fallback_height" }
                    }

            val spanCount = (listWidth / scaledMinItemWidthPx).coerceAtLeast(1)
            val cellSize = listWidth / spanCount.toFloat()
            val maxVisibleRowCount = (listHeight / cellSize).roundToInt()
            val maxRecycledMediaViewCount = maxVisibleRowCount * spanCount * 2

            log.debug {
                "initList(): calculated_grid:" +
                        "\nspanCount=$spanCount," +
                        "\nrowWidth=$listWidth," +
                        "\nitemScale=$itemScale," +
                        "\nminItemWidthPx=$minItemWidthPx," +
                        "\nscaledMinItemWidthPx=$scaledMinItemWidthPx," +
                        "\nmaxVisibleRowCount=$maxVisibleRowCount," +
                        "\nmaxRecycledMediaViewCount=$maxRecycledMediaViewCount"
            }

            // Make items of particular types fill the grid row.
            val gridLayoutManager = GridLayoutManager(context, spanCount).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int =
                        when (galleryAdapter.getItemViewType(position)) {
                            R.id.list_item_gallery_loading_footer,
                            R.layout.list_item_gallery_small_header,
                            R.layout.list_item_gallery_large_header,
                            ->
                                spanCount

                            else ->
                                1
                        }
                }
            }

            adapter = galleryAdapter
            layoutManager = gridLayoutManager

            val endlessScrollListener = object : EndlessRecyclerOnScrollListener(
                footerAdapter = galleryProgressFooterAdapter,
                layoutManager = gridLayoutManager,
                visibleThreshold = gridLayoutManager.spanCount * 5
            ) {
                init {
                    viewModel.isLoading.observe(this@GalleryFolderActivity) { isLoading ->
                        if (isLoading) {
                            disable()
                        } else {
                            enable()
                        }
                    }
                }

                override fun onLoadMore(currentPage: Int) {
                    if (currentPage == 0) {
                        // Filter out false-triggering.
                        return
                    }

                    log.debug {
                        "onLoadMore(): load_more:" +
                                "\npage=$currentPage"
                    }
                    viewModel.loadMore()
                }
            }
            addOnScrollListener(endlessScrollListener)

            // Set the RV pool size and initialize it in background.
            recycledViewPool.setMaxRecycledViews(
                R.id.list_item_gallery_media,
                maxRecycledMediaViewCount
            )
            if (savedInstanceState == null) {
                AsyncRecycledViewPoolInitializer(
                    fastAdapter = galleryAdapter,
                    itemViewType = R.id.list_item_gallery_media,
                    itemViewFactory = GalleryListItem.Media.itemViewFactory,
                    itemViewHolderFactory = GalleryListItem.Media.itemViewHolderFactory,
                )
                    .initPool(
                        recyclerView = view.galleryRecyclerView,
                        recycledViewsCount = maxRecycledMediaViewCount,
                    )
            }

            // Do not run fade animation for changed items.
            // It looks wierd when switching to Selecting from Viewing
            // and vice-versa.
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        val diffCallback = GalleryListItemDiffCallback()
        viewModel.itemsList.observe(this) { newItems ->
            if (newItems != null) {
                FastAdapterDiffUtil.setBetter(
                    recyclerView = view.galleryRecyclerView,
                    adapter = galleryItemsAdapter,
                    items = newItems,
                    callback = diffCallback,
                    detectMoves = false,
                )
            }
        }
    }

     private val GalleryFolderViewModel.Error.localizedMessage: String
        get() = when (this) {
            is GalleryFolderViewModel.Error.LoadingFailed ->
                getString(
                    R.string.template_error_failed_to_load_content,
                    shortSummary,
                )

            GalleryFolderViewModel.Error.NoMediaFound ->
                getString(R.string.no_media_found)
        }


    companion object {
        private const val FALLBACK_LIST_SIZE = 100
        private const val TITLE_EXTRA = "title"
        private const val REPO_PARAMS_EXTRA = "repo-params"

        fun getBundle(
            title: String,
            repositoryParams: SimpleGalleryMediaRepository.Params,
        ) = Bundle().apply {
            putString(TITLE_EXTRA, title)
            putParcelable(REPO_PARAMS_EXTRA, repositoryParams)
        }
    }
}
