package ua.com.radiokot.photoprism.features.gallery.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.core.view.isInvisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.listeners.addClickListener
import com.mikepenz.fastadapter.listeners.addLongClickListener
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityGalleryBinding
import ua.com.radiokot.photoprism.databinding.IncludeActivityGalleryContentBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.ensureItemIsVisible
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.extension.proxyOkResult
import ua.com.radiokot.photoprism.extension.setBetter
import ua.com.radiokot.photoprism.extension.showOverflowItemIcons
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.featureflags.extension.hasMemoriesExtension
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.view.AlbumsActivity
import ua.com.radiokot.photoprism.features.albums.view.DestinationAlbumSelectionActivity
import ua.com.radiokot.photoprism.features.ext.memories.view.GalleryMemoriesListView
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.search.view.GallerySearchBarView
import ua.com.radiokot.photoprism.features.gallery.search.view.GallerySearchView
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryContentLoadingError
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryContentLoadingErrorResources
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryLoadingFooterListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileDownloadActionsViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem
import ua.com.radiokot.photoprism.features.prefs.view.PreferencesActivity
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.features.welcome.data.storage.WelcomeScreenPreferences
import ua.com.radiokot.photoprism.features.welcome.view.WelcomeActivity
import ua.com.radiokot.photoprism.util.AsyncRecycledViewPoolInitializer
import ua.com.radiokot.photoprism.view.ErrorView
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

class GalleryActivity : BaseActivity() {
    private lateinit var rootView: ActivityGalleryBinding
    private lateinit var view: IncludeActivityGalleryContentBinding
    private val viewModel: GalleryViewModel by viewModel()
    private val log = kLogger("GGalleryActivity")
    private var isBackButtonJustPressed = false
    private var isMovedBackByBackButton = false
    private val fileReturnIntentCreator: FileReturnIntentCreator by inject()
    private val welcomeScreenPreferences: WelcomeScreenPreferences by inject()

    private val galleryItemsAdapter = ItemAdapter<GalleryListItem>()
    private val galleryProgressFooterAdapter = ItemAdapter<GalleryLoadingFooterListItem>().apply {
        setNewList(listOf(GalleryLoadingFooterListItem(isLoading = false, canLoadMore = false)))
    }
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
    private var currentListItemScale: GalleryItemScale? = null

    private val mediaFileSelectionView: MediaFileSelectionView by lazy {
        MediaFileSelectionView(
            fragmentManager = supportFragmentManager,
            lifecycleOwner = this
        )
    }
    private val downloadProgressView: DownloadProgressView by lazy {
        DownloadProgressView(
            viewModel = viewModel,
            fragmentManager = supportFragmentManager,
            errorSnackbarView = view.galleryRecyclerView,
            lifecycleOwner = this
        )
    }
    private val searchView: GallerySearchView by lazy {
        GallerySearchView(
            viewModel = viewModel.searchViewModel,
            fragmentManager = supportFragmentManager,
            activity = this,
        )
    }
    private val searchBarView: GallerySearchBarView by lazy {
        GallerySearchBarView(
            viewModel = viewModel.searchViewModel,
            menuRes = R.menu.gallery_search,
            lifecycleOwner = this,
        )
    }
    private val fastScrollView: GalleryFastScrollView by lazy {
        GalleryFastScrollView(
            viewModel = viewModel.fastScrollViewModel,
            lifecycleOwner = this,
        )
    }
    private val memoriesListView: GalleryMemoriesListView by lazy {
        GalleryMemoriesListView(
            viewModel = viewModel.memoriesListViewModel,
            activity = this,
        )
    }
    private val viewerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onViewerResult,
    )
    private val storagePermissionRequestLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            this::onStoragePermissionResult
        )
    private val webViewerForRedirectHandlingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onWebViewerRedirectHandlingResult,
    )
    private val proxyOkResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::proxyOkResult,
    )
    private val addDestinationAlbumSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onAddingDestinationAlbumSelectionResult,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.debug {
            "onCreate(): creating:" +
                    "\naction=${intent.action}," +
                    "\nextras=${intent.extras?.keySet()?.joinToString()}," +
                    "\ntype=${intent.type}" +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        if (intent.action in setOf(Intent.ACTION_GET_CONTENT, Intent.ACTION_PICK)) {
            if (finishIfNoSession()) {
                Toast.makeText(
                    this,
                    R.string.error_you_have_to_connect_to_library,
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            viewModel.initSelectionForAppOnce(
                requestedMimeType = intent.type,
                allowMultiple = intent.extras?.containsKey(Intent.EXTRA_ALLOW_MULTIPLE) == true,
            )
        } else {
            if (!hasSession) {
                if (!welcomeScreenPreferences.isWelcomeNoticeAccepted) {
                    goToWelcomeScreen()
                } else {
                    goToEnvConnection()
                }
                return
            }

            viewModel.initViewingOnce()
        }

        rootView = ActivityGalleryBinding.inflate(layoutInflater)
        view = IncludeActivityGalleryContentBinding.bind(rootView.contentLayout)
        setContentView(rootView.root)

        subscribeToData()
        subscribeToEvents()
        subscribeToState()

        initMediaFileSelection()
        downloadProgressView.init()
        initSearch()
        initNavigation()
        initErrorView()
        initMultipleSelection()
        initSwipeRefresh()

        // Init the list once it is laid out.
        view.galleryRecyclerView.doOnPreDraw {
            initList(savedInstanceState)
        }

        // Allow the view model to intercept back press.
        onBackPressedDispatcher.addCallback(viewModel.backPressedCallback)
    }

    private fun subscribeToData() {
        viewModel.isLoading.observe(this) { isLoading ->
            galleryProgressFooterAdapter[0] = GalleryLoadingFooterListItem(
                isLoading = isLoading,
                canLoadMore = viewModel.canLoadMore
            )

            // Do not show refreshing if there are no gallery items,
            // as in this case the loading footer is on top and visible.
            // It also must not be shown if the recycler is not on the top,
            // for example, when loading subsequent pages.
            view.swipeRefreshLayout.isRefreshing = isLoading
                    && galleryItemsAdapter.adapterItemCount > 0
                    && !view.galleryRecyclerView.canScrollVertically(-1)
        }

        val diffCallback = GalleryListItemDiffCallback()
        viewModel.itemList.observeOnMain().subscribe(this) { newItems ->
            FastAdapterDiffUtil.setBetter(
                recyclerView = view.galleryRecyclerView,
                adapter = galleryItemsAdapter,
                items = newItems,
                callback = diffCallback,
                detectMoves = false,
            )
        }

        viewModel.mainError.observe(this) { error ->
            if (error == null) {
                view.errorView.hide()
                return@observe
            }

            val errorToShow: ErrorView.Error = when (error) {
                GalleryViewModel.Error.NoMediaFound ->
                    ErrorView.Error.EmptyView(
                        messageRes = R.string.no_media_found,
                        context = this,
                    )

                GalleryViewModel.Error.SearchDoesNotFitAllowedTypes ->
                    ErrorView.Error.EmptyView(
                        messageRes = R.string.search_doesnt_fit_allowed_types,
                        context = this,
                    )

                is GalleryViewModel.Error.ContentLoadingError ->
                    when (error.contentLoadingError) {
                        GalleryContentLoadingError.CredentialsHaveBeenChanged,
                        GalleryContentLoadingError.SessionHasBeenExpired ->
                            ErrorView.Error.General(
                                message = error.localizedMessage,
                                imageRes = R.drawable.image_melting,
                                retryButtonText = getString(R.string.disconnect_from_library),
                                retryButtonClickListener = viewModel::onErrorDisconnectClicked
                            )

                        else ->
                            ErrorView.Error.General(
                                message = error.localizedMessage,
                                retryButtonText = getString(R.string.try_again),
                                retryButtonClickListener = viewModel::onMainErrorRetryClicked
                            )
                    }
            }

            view.errorView.showError(errorToShow)
        }

        viewModel.selectedItemsCount.observeOnMain().subscribe(this) { count ->
            view.selectionBottomAppBarTitleTextView.text =
                if (count == 0)
                    getString(R.string.select_content)
                else
                    count.toString()

            updateMultipleSelectionMenuVisibility()
        }

        viewModel.itemScale.observeOnMain().subscribe(this) { itemScale ->
            if (currentListItemScale != null && itemScale != currentListItemScale) {
                // Will not do for pinch gesture.
                lifecycleScope.launchWhenResumed {
                    recreate()
                }
            }
        }

        viewModel.extensionsState
            .skip(1)
            .distinctUntilChanged()
            .subscribe {
                lifecycleScope.launchWhenResumed {
                    recreate()
                }
            }
            .autoDispose(this)
    }

    private fun subscribeToEvents() {
        viewModel.itemListEvents.observeOnMain().subscribe(this) { event ->
            log.debug {
                "subscribeToEvents(): received_list_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is GalleryListViewModel.Event.OpenViewer ->
                    openViewer(
                        mediaIndex = event.mediaIndex,
                        repositoryParams = event.repositoryParams,
                        areActionsEnabled = event.areActionsEnabled,
                    )

                is GalleryListViewModel.Event.EnsureListItemVisible ->
                    view.galleryRecyclerView.post {
                        view.galleryRecyclerView.ensureItemIsVisible(
                            itemGlobalPosition = galleryItemsAdapter.getGlobalPosition(event.listItemIndex)
                        )
                    }

                is GalleryListViewModel.Event.OpenFileSelectionDialog ->
                    openMediaFilesDialog(event.files)
            }

            log.debug {
                "subscribeToEvents(): handled_list_event:" +
                        "\nevent=$event"
            }
        }

        viewModel.mediaFileDownloadActionsEvents.observeOnMain().subscribe(this) { event ->
            log.debug {
                "subscribeToEvents(): received_media_files_actions_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is MediaFileDownloadActionsViewModel.Event.OpenDownloadedFile ->
                    error("Unsupported event")

                MediaFileDownloadActionsViewModel.Event.RequestStoragePermission ->
                    requestStoragePermission()

                is MediaFileDownloadActionsViewModel.Event.ReturnDownloadedFiles ->
                    returnDownloadedFiles(event.files)

                is MediaFileDownloadActionsViewModel.Event.ShareDownloadedFiles ->
                    shareDownloadedFiles(event.files)

                MediaFileDownloadActionsViewModel.Event.ShowFilesDownloadedMessage ->
                    showFloatingMessage(getString(R.string.files_saved_to_downloads))

                MediaFileDownloadActionsViewModel.Event.ShowMissingStoragePermissionMessage ->
                    showFloatingMessage(getString(R.string.error_storage_permission_is_required))
            }

            log.debug {
                "subscribeToEvents(): handled_media_files_actions_event:" +
                        "\nevent=$event"
            }
        }

        viewModel.events.subscribe(this) { event ->
            log.debug {
                "subscribeToEvents(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is GalleryViewModel.Event.ResetScroll -> {
                    resetScroll()
                }

                is GalleryViewModel.Event.ShowFloatingError -> {
                    showFloatingError(event.error)
                }

                is GalleryViewModel.Event.OpenPreferences -> {
                    openPreferences()
                }

                is GalleryViewModel.Event.OpenFolders -> {
                    openFolders()
                }

                is GalleryViewModel.Event.OpenAlbums -> {
                    openAlbums()
                }

                is GalleryViewModel.Event.OpenFavorites -> {
                    openFavorites()
                }

                is GalleryViewModel.Event.GoToEnvConnection -> {
                    goToEnvConnection(
                        rootUrl = event.rootUrl,
                    )
                }

                is GalleryViewModel.Event.OpenWebViewerForRedirectHandling -> {
                    openWebViewerForRedirectHandling(
                        url = event.url,
                    )
                }

                GalleryViewModel.Event.OpenDeletingConfirmationDialog -> {
                    openDeletingConfirmationDialog()
                }

                GalleryViewModel.Event.OpenAddingToAlbumDestinationSelection -> {
                    openAddingDestinationAlbumSelection()
                }

                is GalleryViewModel.Event.ShowFloatingAddedToAlbumMessage -> {
                    showFloatingMessage(
                        getString(
                            R.string.template_selected_added_to_album,
                            event.albumTitle,
                        )
                    )
                }
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }
    }

    private fun subscribeToState() = viewModel.state.subscribe(this) { state ->
        log.debug {
            "subscribeToState(): received_new_state:" +
                    "\nstate=$state"
        }

        title = when (state) {
            is GalleryViewModel.State.Selecting ->
                getString(R.string.select_content)

            GalleryViewModel.State.Viewing ->
                getString(R.string.library)
        }

        with(view.selectionBottomAppBar) {
            // The bottom bar visibility must be switched between Visible and Invisible,
            // because Gone for an unknown reason causes FAB misplacement
            // when switching from Viewing to Selecting 🤷🏻
            isInvisible =
                state is GalleryViewModel.State.Viewing

            navigationIcon =
                if (state is GalleryViewModel.State.Selecting && state.allowMultiple)
                    ContextCompat.getDrawable(this@GalleryActivity, R.drawable.ic_close)
                else
                    null

            updateMultipleSelectionMenuVisibility()
        }

        // The FAB is only used when selecting for other app,
        // as selecting for user allows more than 1 action.
        if (state is GalleryViewModel.State.Selecting.ForOtherApp) {
            viewModel.selectedItemsCount.observeOnMain().subscribe(this) { count ->
                if (count > 0) {
                    view.doneSelectingFab.show()
                } else {
                    view.doneSelectingFab.hide()
                }
            }
        } else {
            view.doneSelectingFab.hide()
        }

        log.debug {
            "subscribeToState(): handled_new_state:" +
                    "\nstate=$state"
        }
    }

    private fun initList(savedInstanceState: Bundle?) {
        val galleryAdapter = FastAdapter.with(
            if (get<FeatureFlags>().hasMemoriesExtension)
                listOf(
                    memoriesListView.recyclerAdapter,
                    galleryItemsAdapter,
                    galleryProgressFooterAdapter
                )
            else
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
                        is GalleryListItem.Media ->
                            when (view.id) {
                                R.id.view_button ->
                                    viewModel.onGalleryMediaItemViewClicked(item)

                                else ->
                                    viewModel.onGalleryMediaItemClicked(item)
                            }

                        is GalleryLoadingFooterListItem ->
                            viewModel.onLoadingFooterLoadMoreClicked()
                    }
                }
            )

            addLongClickListener(
                resolveView = { viewHolder: ViewHolder ->
                    (viewHolder as? GalleryListItem.Media.ViewHolder)
                        ?.itemView
                },
                onLongClick = { _, _, _, item ->
                    if (item !is GalleryListItem.Media) {
                        return@addLongClickListener false
                    }

                    viewModel.onGalleryMediaItemLongClicked(item)
                    true
                }
            )
        }

        val itemScale = viewModel.itemScale.value!!
        currentListItemScale = itemScale
        val minItemWidthPx =
            resources.getDimensionPixelSize(R.dimen.list_item_gallery_media_min_size)
        val scaledMinItemWidthPx = ceil(minItemWidthPx * itemScale.factor)
            .toInt()
            .coerceAtLeast(1)

        with(view.galleryRecyclerView) {
            setPadding(
                paddingLeft,
                view.searchBar.bottom,
                paddingRight,
                paddingBottom
            )

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
                            GalleryMemoriesListView.RECYCLER_VIEW_TYPE,
                            ->
                                spanCount

                            else ->
                                1
                        }
                }
            }

            adapter = galleryAdapter
            layoutManager = gridLayoutManager

            endlessScrollListener = object : EndlessRecyclerOnScrollListener(
                footerAdapter = galleryProgressFooterAdapter,
                layoutManager = gridLayoutManager,
                visibleThreshold = gridLayoutManager.spanCount * 5
            ) {
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
            viewModel.isLoading.observe(this@GalleryActivity) { isLoading ->
                if (isLoading) {
                    endlessScrollListener.disable()
                } else {
                    endlessScrollListener.enable()
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

        fastScrollView.init(
            fastScrollRecyclerView = view.galleryRecyclerView,
        )
    }

    private fun initMediaFileSelection() {
        mediaFileSelectionView.init { fileItem ->
            if (fileItem.source != null) {
                viewModel.onGalleryMediaFileSelected(fileItem.source)
            }
        }
    }

    private fun initSearch() {
        searchView.init(
            searchView = view.searchView,
            configView = view.searchContent,
        )
        searchBarView.init(
            searchBar = view.searchBar,
        )
    }

    private fun initNavigation() {
        GalleryNavigationView(
            viewModel = viewModel,
        ).apply {
            val drawerLayout = rootView.root as? DrawerLayout
            val navigationView = rootView.navigationView
            val navigationRail = rootView.navigationRail

            when {
                drawerLayout != null && navigationView != null ->
                    initWithDrawer(
                        drawerLayout = drawerLayout,
                        navigationView = navigationView,
                        searchBarView = searchBarView,
                    )

                navigationRail != null ->
                    initWithRail(
                        navigationRail = navigationRail,
                    )

                else ->
                    error("The layout must have a navigation view")
            }
        }
    }

    private fun initErrorView() {
        view.errorView.replaces(view.galleryRecyclerView)
    }

    private fun initMultipleSelection() {
        with(view.selectionBottomAppBar) {
            setNavigationOnClickListener {
                viewModel.onClearSelectionClicked()
            }

            menu.showOverflowItemIcons(isBottomBar = true)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.share ->
                        viewModel.onShareMultipleSelectionClicked()

                    R.id.download ->
                        viewModel.onDownloadMultipleSelectionClicked()

                    R.id.add_to_album ->
                        viewModel.onAddToAlbumMultipleSelectionClicked()

                    R.id.archive ->
                        viewModel.onArchiveMultipleSelectionClicked()

                    R.id.delete ->
                        viewModel.onDeleteMultipleSelectionClicked()
                }

                true
            }
        }

        view.doneSelectingFab.setOnClickListener {
            viewModel.onDoneMultipleSelectionClicked()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            ShareSheetShareEventReceiver.shareEvents.subscribe(this) {
                viewModel.onDownloadedFilesShared()
            }
        }
    }

    private fun initSwipeRefresh() = with(view.swipeRefreshLayout) {
        setProgressViewOffset(
            false,
            progressViewStartOffset
                    + resources.getDimensionPixelSize(R.dimen.gallery_swipe_refresh_start_offset),
            progressViewEndOffset
                    + resources.getDimensionPixelSize(R.dimen.gallery_swipe_refresh_end_offset),
        )
        setOnRefreshListener(viewModel::onSwipeRefreshPulled)
    }

    private fun updateMultipleSelectionMenuVisibility() {
        val multipleSelectionItemsCount = viewModel.selectedItemsCount.value ?: 0
        val state = viewModel.currentState
        val areUserSelectionItemsVisible =
            multipleSelectionItemsCount > 0 && state is GalleryViewModel.State.Selecting.ForUser

        with(view.selectionBottomAppBar.menu) {
            forEach { menuItem ->
                menuItem.isVisible = areUserSelectionItemsVisible
            }
        }
    }

    private fun openMediaFilesDialog(files: List<GalleryMedia.File>) {
        mediaFileSelectionView.openMediaFileSelectionDialog(
            fileItems = files.map {
                MediaFileListItem(
                    source = it,
                    context = this
                )
            }
        )
    }

    private fun returnDownloadedFiles(
        filesToReturn: List<SendableFile>,
    ) {
        val resultIntent = fileReturnIntentCreator.createIntent(filesToReturn)
        setResult(RESULT_OK, resultIntent)

        log.debug {
            "returnDownloadedFiles(): result_set_finishing:" +
                    "\nintent=$resultIntent," +
                    "\nfilesToReturnCount=${filesToReturn.size}"
        }

        finish()
    }

    private fun shareDownloadedFiles(
        files: List<SendableFile>,
    ) {
        val resultIntent = fileReturnIntentCreator.createIntent(files)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // From Android 5.1 it is possible to get a callback on successful sharing,
            // which is used to exit Selection once the files are shared.
            val callbackPendingIntent = ShareSheetShareEventReceiver.getPendingIntent(this)

            log.debug {
                "shareDownloadedFiles(): starting_intent_with_callback:" +
                        "\nintent=$resultIntent," +
                        "\ncallbackPendingIntent=$callbackPendingIntent," +
                        "\nfilesCount=${files.size}"
            }

            startActivity(
                Intent.createChooser(
                    resultIntent,
                    getString(R.string.share),
                    callbackPendingIntent.intentSender
                )
            )
        } else {
            // If there is no way to determine whether the files are shared,
            // just assume they are once the dialog is opened.

            log.debug {
                "shareDownloadedFiles(): starting_intent:" +
                        "\nintent=$resultIntent," +
                        "\nfilesCount=${files.size}"
            }

            startActivity(
                Intent.createChooser(
                    resultIntent,
                    getString(R.string.share),
                )
            )

            viewModel.onDownloadedFilesShared()
        }
    }

    private fun openViewer(
        mediaIndex: Int,
        repositoryParams: SimpleGalleryMediaRepository.Params,
        areActionsEnabled: Boolean,
    ) {
        viewerLauncher.launch(
            Intent(this, MediaViewerActivity::class.java)
                .putExtras(
                    MediaViewerActivity.getBundle(
                        mediaIndex = mediaIndex,
                        repositoryParams = repositoryParams,
                        areActionsEnabled = areActionsEnabled,
                    )
                )
        )
    }

    private fun onViewerResult(result: ActivityResult) {
        val lastViewedMediaIndex = MediaViewerActivity.getResult(result)
            ?: return

        viewModel.onViewerReturnedLastViewedMediaIndex(lastViewedMediaIndex)
    }

    private fun openPreferences() {
        startActivity(Intent(this, PreferencesActivity::class.java))
    }

    private fun openFolders() {
        proxyOkResultLauncher.launch(
            Intent(this, AlbumsActivity::class.java)
                .putExtras(intent.extras ?: Bundle())
                .putExtras(
                    AlbumsActivity.getBundle(
                        albumType = Album.TypeName.FOLDER,
                    )
                )
                .setAction(intent.action)
        )
    }

    private fun openAlbums() {
        proxyOkResultLauncher.launch(
            Intent(this, AlbumsActivity::class.java)
                .putExtras(intent.extras ?: Bundle())
                .putExtras(
                    AlbumsActivity.getBundle(
                        albumType = Album.TypeName.ALBUM,
                    )
                )
                .setAction(intent.action)
        )
    }

    private fun openFavorites() {
        proxyOkResultLauncher.launch(
            Intent(this, GallerySingleRepositoryActivity::class.java)
                .putExtras(intent.extras ?: Bundle())
                .putExtras(
                    GallerySingleRepositoryActivity.getBundle(
                        title = getString(R.string.favorites),
                        repositoryParams = SimpleGalleryMediaRepository.Params(
                            searchConfig = SearchConfig.DEFAULT.copy(
                                onlyFavorite = true,
                            )
                        ),
                    )
                )
                .setAction(intent.action)
        )
    }

    private fun resetScroll() {
        log.debug {
            "resetScroll(): resetting_scroll"
        }

        with(view.galleryRecyclerView) {
            scrollToPosition(0)
            endlessScrollListener.resetPageCount(0)
        }
    }

    private fun showFloatingError(error: GalleryViewModel.Error) {
        Snackbar.make(view.galleryRecyclerView, error.localizedMessage, Snackbar.LENGTH_SHORT)
            .setAction(R.string.try_again) { viewModel.onFloatingErrorRetryClicked() }
            .show()
    }

    private fun showFloatingMessage(message: String) {
        Snackbar.make(view.galleryRecyclerView, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun requestStoragePermission() {
        storagePermissionRequestLauncher.launch(Unit)
    }

    private fun onStoragePermissionResult(isGranted: Boolean) {
        viewModel.onStoragePermissionResult(isGranted)
    }

    private fun goToWelcomeScreen() {
        log.debug {
            "goToWelcomeScreen(): going_to_welcome_screen"
        }

        startActivity(Intent(this, WelcomeActivity::class.java))
        finishAffinity()
    }

    private fun openWebViewerForRedirectHandling(url: String) =
        webViewerForRedirectHandlingLauncher.launch(
            Intent(this, WebViewActivity::class.java)
                .putExtras(
                    WebViewActivity.getBundle(
                        url = url,
                        titleRes = R.string.connect_to_a_library,
                        finishOnRedirectEnd = true,
                    )
                )
        )

    private fun onWebViewerRedirectHandlingResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onWebViewerHandledRedirect()
        }
    }

    private fun openAddingDestinationAlbumSelection() {
        addDestinationAlbumSelectionLauncher.launch(
            Intent(this, DestinationAlbumSelectionActivity::class.java)
                .putExtras(
                    DestinationAlbumSelectionActivity.getBundle(
                        selectedAlbums = emptySet(),
                        isSingleSelection = true,
                    )
                )
        )
    }

    private fun onAddingDestinationAlbumSelectionResult(result: ActivityResult) {
        val bundle = result.data?.extras
        if (result.resultCode == Activity.RESULT_OK && bundle != null) {
            viewModel.onAddToAlbumMultipleSelectionDestinationSelected(
                selectedAlbum = DestinationAlbumSelectionActivity
                    .getSelectedAlbums(bundle)
                    .first()
            )
        }
    }

    private fun openDeletingConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.gallery_deleting_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.onDeletingMultipleSelectionConfirmed()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private var backPressResetDisposable: Disposable? = null
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backPressResetDisposable?.dispose()
            backPressResetDisposable = Single.timer(400, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .doOnSubscribe { isBackButtonJustPressed = true }
                .subscribeBy { isBackButtonJustPressed = false }
                .autoDispose(this)
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        // Detect moving to background to handle it on resume.
        // If there is a better solution, I am happy to use it.
        isMovedBackByBackButton = !isFinishing && isBackButtonJustPressed
    }

    override fun onResume() {
        super.onResume()
        if (isMovedBackByBackButton) {
            viewModel.onScreenResumedAfterMovedBackWithBackButton()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        // Hope it works for TV remotes, I can't test it.
        in setOf(
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_BOOKMARK,
            KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1, // "Context menu" button
            KeyEvent.KEYCODE_CHANNEL_DOWN,
        ) -> {
            viewModel.onPreferencesClicked()
            true
        }

        in setOf(
            KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_CHANNEL_UP,
        ) -> {
            view.searchBar.callOnClick()
            true
        }

        else ->
            super.onKeyDown(keyCode, event)
    }

    private val GalleryViewModel.Error.localizedMessage: String
        get() = when (this) {
            GalleryViewModel.Error.NoMediaFound ->
                getString(R.string.no_media_found)

            GalleryViewModel.Error.SearchDoesNotFitAllowedTypes ->
                getString(R.string.search_doesnt_fit_allowed_types)

            is GalleryViewModel.Error.ContentLoadingError ->
                GalleryContentLoadingErrorResources.getMessage(
                    error = contentLoadingError,
                    context = this@GalleryActivity,
                )
        }

    private companion object {
        private const val FALLBACK_LIST_SIZE = 100
    }
}
