package ua.com.radiokot.photoprism.features.gallery.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.core.view.isInvisible
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
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityGallerySingleRepositoryBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.ensureItemIsVisible
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.extension.setBetter
import ua.com.radiokot.photoprism.extension.showOverflowItemIcons
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.albums.view.DestinationAlbumSelectionActivity
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryContentLoadingErrorResources
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryLoadingFooterListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaRemoteActionsViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySingleRepositoryViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileDownloadActionsViewModel
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity
import ua.com.radiokot.photoprism.util.AsyncRecycledViewPoolInitializer
import ua.com.radiokot.photoprism.view.ErrorView
import kotlin.math.ceil
import kotlin.math.roundToInt

class GallerySingleRepositoryActivity : BaseActivity() {

    private val log = kLogger("GallerySingleRepositoryActivity")
    private lateinit var view: ActivityGallerySingleRepositoryBinding
    private val viewModel: GallerySingleRepositoryViewModel by viewModel()
    private val galleryItemsAdapter = ItemAdapter<GalleryListItem>()
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
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
    private val addDestinationAlbumSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onAddingDestinationAlbumSelectionResult,
    )
    private val fileReturnIntentCreator: FileReturnIntentCreator by inject()
    private val downloadProgressView: DownloadProgressView by lazy {
        DownloadProgressView(
            viewModel = viewModel,
            fragmentManager = supportFragmentManager,
            errorSnackbarView = view.galleryRecyclerView,
            lifecycleOwner = this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityGallerySingleRepositoryBinding.inflate(layoutInflater)
        setContentView(view.root)

        @Suppress("DEPRECATION")
        val repositoryParams: SimpleGalleryMediaRepository.Params =
            requireNotNull(intent.getParcelableExtra(REPO_PARAMS_EXTRA)) {
                "No repository params specified"
            }
        if (intent.action in setOf(Intent.ACTION_GET_CONTENT, Intent.ACTION_PICK)) {
            viewModel.initSelectionForAppOnce(
                repositoryParams = repositoryParams,
                allowMultiple = intent.extras?.containsKey(Intent.EXTRA_ALLOW_MULTIPLE) == true,
            )
        } else {
            viewModel.initViewingOnce(
                repositoryParams = repositoryParams,
            )
        }

        // Init the list once it is laid out.
        view.galleryRecyclerView.doOnPreDraw {
            initList(savedInstanceState)
        }
        initToolbar()
        initSwipeRefresh()
        initErrorView()
        initMultipleSelection()
        downloadProgressView.init()

        subscribeToEvents()

        // Allow the view model to intercept back press.
        onBackPressedDispatcher.addCallback(viewModel.backPressedCallback)
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent.getStringExtra(TITLE_EXTRA)?.also(::setTitle)
    }

    private fun initSwipeRefresh() = with(view.swipeRefreshLayout) {
        setOnRefreshListener(viewModel::onSwipeRefreshPulled)

        viewModel.isLoading.observe(this@GallerySingleRepositoryActivity) { isLoading ->
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
        viewModel.mainError.observe(this) { error ->
            if (error == null) {
                view.errorView.hide()
                return@observe
            }

            val errorToShow: ErrorView.Error = when (error) {
                GallerySingleRepositoryViewModel.Error.NoMediaFound ->
                    ErrorView.Error.EmptyView(
                        message = error.localizedMessage,
                    )

                else ->
                    ErrorView.Error.General(
                        message = error.localizedMessage,
                        retryButtonText = getString(R.string.try_again),
                        retryButtonClickListener = viewModel::onMainErrorRetryClicked
                    )
            }

            view.errorView.showError(errorToShow)
        }
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

            viewModel.state.subscribeBy { state ->
                // The bottom bar visibility must be switched between Visible and Invisible,
                // because Gone for an unknown reason causes FAB misplacement
                // when switching from Viewing to Selecting 🤷🏻
                isInvisible =
                    state is GallerySingleRepositoryViewModel.State.Viewing

                navigationIcon =
                    if (state is GallerySingleRepositoryViewModel.State.Selecting && state.allowMultiple)
                        ContextCompat.getDrawable(
                            this@GallerySingleRepositoryActivity,
                            R.drawable.ic_close
                        )
                    else
                        null

                updateMultipleSelectionMenuVisibility()
            }
        }

        view.doneSelectingFab.setOnClickListener {
            viewModel.onDoneMultipleSelectionClicked()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            ShareSheetShareEventReceiver.shareEvents.subscribe(this) {
                viewModel.onDownloadedMediaFilesShared()
            }
        }

        viewModel.selectedItemsCount.observeOnMain().subscribe(this) { count ->
            view.selectionBottomAppBarTitleTextView.text =
                if (count == 0)
                    getString(R.string.select_content)
                else
                    count.toString()

            updateMultipleSelectionMenuVisibility()
        }

        viewModel.state.subscribeBy { state ->
            // The FAB is only used when selecting for other app,
            // as selecting for user allows more than 1 action.
            if (state is GallerySingleRepositoryViewModel.State.Selecting.ForOtherApp) {
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
        }.autoDispose(this)
    }

    private fun updateMultipleSelectionMenuVisibility() {
        val multipleSelectionItemsCount = viewModel.selectedItemsCount.value ?: 0
        val state = viewModel.currentState
        val areUserSelectionItemsVisible =
            multipleSelectionItemsCount > 0 && state is GallerySingleRepositoryViewModel.State.Selecting.ForUser

        with(view.selectionBottomAppBar.menu) {
            forEach { menuItem ->
                menuItem.isVisible = areUserSelectionItemsVisible
            }
        }
    }

    private fun initList(savedInstanceState: Bundle?) {
        val galleryProgressFooterAdapter = ItemAdapter<GalleryLoadingFooterListItem>().apply {
            setNewList(listOf(GalleryLoadingFooterListItem(isLoading = false, canLoadMore = false)))

            viewModel.isLoading.observe(this@GallerySingleRepositoryActivity) { isLoading ->
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

            endlessScrollListener = object : EndlessRecyclerOnScrollListener(
                footerAdapter = galleryProgressFooterAdapter,
                layoutManager = gridLayoutManager,
                visibleThreshold = gridLayoutManager.spanCount * 5
            ) {
                init {
                    viewModel.isLoading.observe(this@GallerySingleRepositoryActivity) { isLoading ->
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
        viewModel.itemList.observeOnMain().subscribe(this@GallerySingleRepositoryActivity) { newItems ->
            FastAdapterDiffUtil.setBetter(
                recyclerView = view.galleryRecyclerView,
                adapter = galleryItemsAdapter,
                items = newItems,
                callback = diffCallback,
                detectMoves = false,
            )
        }
    }

    private fun subscribeToEvents() {
        viewModel.itemListEvents.observeOnMain().subscribe(this) { event ->
            log.debug {
                "subscribeToEvents(): received_item_list_event:" +
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
            }

            log.debug {
                "subscribeToEvents(): handled_item_list_event:" +
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

        viewModel.galleryMediaRemoteActionsEvents.observeOnMain().subscribe(this) { event ->
            log.debug {
                "subscribeToEvents(): received_gallery_media_remote_actions_event:" +
                        "\nevent=$event"
            }

            when (event) {
                GalleryMediaRemoteActionsViewModel.Event.OpenAlbumForAddingSelection ->
                    openAddingDestinationAlbumSelection()

                GalleryMediaRemoteActionsViewModel.Event.OpenDeletingConfirmationDialog ->
                    openDeletingConfirmationDialog()

                is GalleryMediaRemoteActionsViewModel.Event.ShowFloatingAddedToAlbumMessage ->
                    showFloatingMessage(
                        getString(
                            R.string.template_selected_added_to_album,
                            event.albumTitle,
                        )
                    )
            }

            log.debug {
                "subscribeToEvents(): handled_gallery_media_remote_actions_event:" +
                        "\nevent=$event"
            }
        }

        viewModel.events.subscribe(this) { event ->
            log.debug {
                "subscribeToEvents(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is GallerySingleRepositoryViewModel.Event.ResetScroll -> {
                    resetScroll()
                }

                is GallerySingleRepositoryViewModel.Event.ShowFloatingError ->
                    showFloatingError(event.error)
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }
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

    private fun showFloatingError(error: GallerySingleRepositoryViewModel.Error) {
        Snackbar.make(view.galleryRecyclerView, error.localizedMessage, Snackbar.LENGTH_SHORT)
            .setAction(R.string.try_again) { viewModel.onFloatingErrorRetryClicked() }
            .show()
    }

    private fun showFloatingMessage(message: String) {
        Snackbar.make(view.galleryRecyclerView, message, Snackbar.LENGTH_SHORT)
            .show()
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
            viewModel.onAlbumForAddingGalleryMediaSelected(
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
                viewModel.onDeletingGalleryMediaConfirmed()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

            viewModel.onDownloadedMediaFilesShared()
        }
    }

    private fun requestStoragePermission() {
        storagePermissionRequestLauncher.launch(Unit)
    }

    private fun onStoragePermissionResult(isGranted: Boolean) {
        viewModel.onStoragePermissionResult(isGranted)
    }

    private val GallerySingleRepositoryViewModel.Error.localizedMessage: String
        get() = when (this) {
            GallerySingleRepositoryViewModel.Error.NoMediaFound ->
                getString(R.string.no_media_found)

            is GallerySingleRepositoryViewModel.Error.ContentLoadingError ->
                GalleryContentLoadingErrorResources.getMessage(
                    error = contentLoadingError,
                    context = this@GallerySingleRepositoryActivity,
                )
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
