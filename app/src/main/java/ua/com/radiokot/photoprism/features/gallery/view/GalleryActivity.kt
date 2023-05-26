package ua.com.radiokot.photoprism.features.gallery.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.listeners.addClickListener
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityGalleryBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.features.envconnection.view.EnvConnectionActivity
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryLoadingFooterListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem
import ua.com.radiokot.photoprism.features.prefs.view.PreferencesActivity
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity
import ua.com.radiokot.photoprism.util.AsyncListItemViewCache
import ua.com.radiokot.photoprism.view.ErrorView


class GalleryActivity : BaseActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            linkTo(getScope(DI_SCOPE_SESSION))
        }
    }

    private lateinit var view: ActivityGalleryBinding
    private val viewModel: GalleryViewModel by viewModel()
    private val log = kLogger("GGalleryActivity")

    private val galleryItemsAdapter = ItemAdapter<GalleryListItem>()
    private val galleryProgressFooterAdapter = ItemAdapter<GalleryLoadingFooterListItem>()
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
    private val galleryMediaListItemViewCache =
        AsyncListItemViewCache(GalleryListItem.Media.viewFactory)

    private val fileReturnIntentCreator: FileReturnIntentCreator by inject()

    private val mediaFileSelectionView: MediaFileSelectionView by lazy {
        MediaFileSelectionView(
            fragmentManager = supportFragmentManager,
            lifecycleOwner = this
        )
    }
    private val downloadProgressView: DownloadProgressView by lazy {
        DownloadProgressView(
            viewModel = viewModel.downloadMediaFileViewModel,
            fragmentManager = supportFragmentManager,
            errorSnackbarView = view.galleryRecyclerView,
            lifecycleOwner = this
        )
    }
    private val searchView: GallerySearchView by lazy {
        GallerySearchView(
            viewModel = viewModel.searchViewModel,
            fragmentManager = supportFragmentManager,
            menuRes = R.menu.gallery,
            lifecycleOwner = this
        )
    }
    private val fastScrollView: GalleryFastScrollView by lazy {
        GalleryFastScrollView(
            viewModel = viewModel.fastScrollViewModel,
            lifecycleOwner = this,
        )
    }

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
            if (tryOrNull { scope } == null) {
                log.warn {
                    "onCreate(): no_scope_finishing"
                }

                Toast.makeText(
                    this,
                    R.string.error_you_have_to_connect_to_library,
                    Toast.LENGTH_SHORT
                ).show()

                finish()
                return
            }

            viewModel.initSelectionOnce(
                requestedMimeType = intent.type,
                allowMultiple = intent.extras?.containsKey(Intent.EXTRA_ALLOW_MULTIPLE) == true,
            )
        } else {
            if (tryOrNull { scope } == null) {
                log.warn {
                    "onCreate(): no_scope_going_to_env_connection"
                }
                goToEnvConnection()
                return
            }

            viewModel.initViewingOnce()
        }

        view = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(view.root)

        subscribeToData()
        subscribeToEvents()
        subscribeToState()

        view.galleryRecyclerView.post(::initList)
        initMediaFileSelection()
        downloadProgressView.init()
        initSearch()
        initErrorView()
        initMultipleSelection()
    }

    private fun subscribeToData() {
        viewModel.isLoading.observe(this) { isLoading ->
            galleryProgressFooterAdapter.setNewList(
                listOf(
                    GalleryLoadingFooterListItem(
                        isLoading = isLoading,
                        canLoadMore = viewModel.canLoadMore
                    )
                )
            )
        }

        val galleryItemsDiffCallback = GalleryListItemDiffCallback()
        viewModel.itemsList.observe(this) { newItems ->
            if (newItems != null) {
                newItems.forEach {
                    if (it is GalleryListItem.Media) {
                        it.viewCache = galleryMediaListItemViewCache
                    }
                }

                if (galleryItemsAdapter.adapterItemCount == 0 || newItems.isEmpty()) {
                    // Do not use DiffUtil to replace an empty list,
                    // as it causes scrolling to the bottom.
                    // Do not use it to set an empty list either,
                    // as it causes an unnecessary "delete" animation.
                    galleryItemsAdapter.setNewList(newItems)
                } else {
                    FastAdapterDiffUtil.set(
                        adapter = galleryItemsAdapter,
                        items = newItems,
                        callback = galleryItemsDiffCallback,
                        detectMoves = true,
                    )
                }
            }
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
                GalleryViewModel.Error.SearchDoesntFitAllowedTypes ->
                    ErrorView.Error.EmptyView(
                        messageRes = R.string.search_doesnt_fit_allowed_types,
                        context = this,
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

        viewModel.multipleSelectionItemsCount.observe(this) { count ->
            view.selectionBottomAppBarTitleTextView.text =
                if (count == 0)
                    getString(R.string.select_content)
                else
                    count.toString()
            if (count > 0) {
                view.doneSelectingFab.show()
            } else {
                view.doneSelectingFab.hide()
            }
        }
    }

    private fun subscribeToEvents() {
        viewModel.events.subscribe { event ->
            log.debug {
                "subscribeToEvents(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is GalleryViewModel.Event.OpenFileSelectionDialog ->
                    openMediaFilesDialog(
                        files = event.files,
                    )

                is GalleryViewModel.Event.ReturnDownloadedFiles ->
                    returnDownloadedFiles(event.files)

                is GalleryViewModel.Event.OpenViewer ->
                    openViewer(
                        mediaIndex = event.mediaIndex,
                        repositoryQuery = event.repositoryQuery,
                        areActionsEnabled = event.areActionsEnabled,
                    )

                is GalleryViewModel.Event.ResetScroll -> {
                    resetScroll()
                }

                is GalleryViewModel.Event.ShowFloatingError -> {
                    showFloatingError(event.error)
                }

                is GalleryViewModel.Event.OpenPreferences -> {
                    openPreferences()
                }
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.autoDispose(this)
    }

    private fun subscribeToState() {
        viewModel.state.subscribeBy { state ->
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

            view.selectionBottomAppBar.isVisible =
                state is GalleryViewModel.State.Selecting
            view.selectionBottomAppBar.navigationIcon =
                if (state is GalleryViewModel.State.Selecting && state.allowMultiple)
                    ContextCompat.getDrawable(this, R.drawable.ic_close)
                else
                    null

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.autoDispose(this)
    }

    private fun initList() {
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
                                R.id.view_button ->
                                    viewModel.onItemViewButtonClicked(item)
                                else ->
                                    viewModel.onItemClicked(item)
                            }
                        is GalleryLoadingFooterListItem ->
                            viewModel.onLoadingFooterLoadMoreClicked()
                    }
                }
            )
        }

        with(view.galleryRecyclerView) {
            setPadding(
                paddingLeft,
                view.searchBar.bottom,
                paddingRight,
                paddingBottom
            )

            val minItemWidthPx =
                resources.getDimensionPixelSize(R.dimen.list_item_gallery_media_min_size)
            val rowWidth = measuredWidth
            val spanCount = (rowWidth / minItemWidthPx).coerceAtLeast(1)

            log.debug {
                "initList(): calculated_span_count:" +
                        "\nspanCount=$spanCount," +
                        "\nrowWidth=$rowWidth," +
                        "\nminItemWidthPx=$minItemWidthPx"
            }

            // Make items of particular types fill the grid row.
            val gridLayoutManager = GridLayoutManager(context, spanCount).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int =
                        when (galleryAdapter.getItemViewType(position)) {
                            R.id.list_item_gallery_loading_footer,
                            R.id.list_item_gallery_day_header,
                            R.id.list_item_month_header ->
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
        }

        fastScrollView.init(
            fastScrollRecyclerView = view.galleryRecyclerView,
        )

        galleryMediaListItemViewCache.populateCache(
            context = this,
            parent = view.galleryRecyclerView,
            count = 30,
        )
    }

    private fun initMediaFileSelection() {
        mediaFileSelectionView.init { fileItem ->
            if (fileItem.source != null) {
                viewModel.onFileSelected(fileItem.source)
            }
        }
    }

    private fun initSearch() {
        searchView.init(
            searchBar = view.searchBar,
            searchView = view.searchView,
            configurationView = view.searchContent,
        )

        onBackPressedDispatcher.addCallback(this, searchView.searchResetBackPressedCallback)
        onBackPressedDispatcher.addCallback(this, searchView.closeConfigurationBackPressedCallback)

        view.searchBar.setNavigationOnClickListener {
            viewModel.onPreferencesButtonClicked()
        }
    }

    private fun initErrorView() {
        view.errorView.replaces(view.galleryRecyclerView)
    }

    private fun initMultipleSelection() {
        view.selectionBottomAppBar.setNavigationOnClickListener {
            viewModel.onClearMultipleSelectionClicked()
        }

        view.doneSelectingFab.setOnClickListener {
            viewModel.onDoneMultipleSelectionClicked()
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
        filesToReturn: List<GalleryViewModel.Event.ReturnDownloadedFiles.FileToReturn>,
    ) {
        val resultIntent = fileReturnIntentCreator.createIntent(
            filesToReturn.map { fileToReturn ->
                FileReturnIntentCreator.FileToReturn(
                    file = fileToReturn.downloadedFile,
                    mimeType = fileToReturn.mimeType,
                    displayName = fileToReturn.displayName,
                )
            }
        )
        setResult(RESULT_OK, resultIntent)

        log.debug {
            "returnDownloadedFiles(): result_set_finishing:" +
                    "\nintent=$resultIntent," +
                    "\nfilesToReturnCount=${filesToReturn.size}"
        }

        finish()
    }

    private fun openViewer(
        mediaIndex: Int,
        repositoryQuery: String?,
        areActionsEnabled: Boolean,
    ) {
        startActivity(
            Intent(this, MediaViewerActivity::class.java)
                .putExtras(
                    MediaViewerActivity.getBundle(
                        mediaIndex = mediaIndex,
                        repositoryQuery = repositoryQuery,
                        areActionsEnabled = areActionsEnabled,
                    )
                )
        )
    }

    private fun openPreferences() {
        startActivity(Intent(this, PreferencesActivity::class.java))
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

    private fun goToEnvConnection() {
        log.debug {
            "goToEnvConnection(): going_to_env_connection"
        }

        startActivity(Intent(this, EnvConnectionActivity::class.java))
        finish()
    }

    private fun showFloatingError(error: GalleryViewModel.Error) {
        Snackbar.make(view.galleryRecyclerView, error.localizedMessage, Snackbar.LENGTH_SHORT)
            .setAction(R.string.try_again) { viewModel.onFloatingErrorRetryClicked() }
            .show()
    }

    private val GalleryViewModel.Error.localizedMessage: String
        get() = when (this) {
            GalleryViewModel.Error.LibraryNotAccessible ->
                getString(R.string.error_library_not_accessible_try_again)
            is GalleryViewModel.Error.LoadingFailed ->
                getString(
                    R.string.template_error_failed_to_load_content,
                    shortSummary,
                )
            GalleryViewModel.Error.NoMediaFound ->
                getString(R.string.no_media_found)
            GalleryViewModel.Error.SearchDoesntFitAllowedTypes ->
                getString(R.string.search_doesnt_fit_allowed_types)
        }
}