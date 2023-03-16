package ua.com.radiokot.photoprism.features.gallery.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.forEach
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityGalleryBinding
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryProgressListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity
import java.io.File


class GalleryActivity : BaseActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            linkTo(getScope("session"))
        }
    }

    private lateinit var view: ActivityGalleryBinding
    private val viewModel: GalleryViewModel by viewModel()
    private val downloadViewModel: DownloadMediaFileViewModel by viewModel()
    private val searchViewModel: GallerySearchViewModel by viewModel()
    private val log = kLogger("GGalleryActivity")

    private val galleryItemsAdapter = ItemAdapter<GalleryListItem>()
    private val galleryProgressFooterAdapter = ItemAdapter<GalleryProgressListItem>()

    private val fileReturnIntentCreator: FileReturnIntentCreator by inject()

    private val mediaFileSelectionView: MediaFileSelectionView by lazy {
        MediaFileSelectionView(
            fragmentManager = supportFragmentManager,
            lifecycleOwner = this
        )
    }
    private val downloadProgressView: DownloadProgressView by lazy {
        DownloadProgressView(
            viewModel = downloadViewModel,
            fragmentManager = supportFragmentManager,
            errorSnackbarView = view.galleryRecyclerView,
            lifecycleOwner = this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.debug {
            "onCreate(): creating:" +
                    "\naction=${intent.action}," +
                    "\nextras=${intent.extras}," +
                    "\ntype=${intent.type}" +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        if (intent.action in setOf(Intent.ACTION_GET_CONTENT, Intent.ACTION_PICK)) {
            viewModel.initSelection(
                downloadViewModel = downloadViewModel,
                requestedMimeType = intent.type,
            )
        } else {
            viewModel.initViewing(
                downloadViewModel = downloadViewModel,
            )
        }

        view = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(view.root)

        subscribeToData()
        subscribeToEvents()
        subscribeToState()
        subscribeToSearch()

        view.galleryRecyclerView.post(::initList)
        initMediaFileSelection()
        downloadProgressView.init()
    }

    private fun subscribeToData() {
        viewModel.isLoading
            .observe(this) { isLoading ->
                if (!isLoading) {
                    galleryProgressFooterAdapter.clear()
                } else if (galleryProgressFooterAdapter.adapterItemCount == 0) {
                    galleryProgressFooterAdapter.add(GalleryProgressListItem())
                }
            }

        viewModel.itemsList
            .observe(this) {
                if (it != null) {
                    galleryItemsAdapter.setNewList(it)
                }
            }
    }

    private fun subscribeToEvents() {
        viewModel.events
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                log.debug {
                    "subscribeToEvents(): received_new_event:" +
                            "\nevent=$event"
                }

                when (event) {
                    is GalleryViewModel.Event.OpenFileSelectionDialog ->
                        openMediaFilesDialog(
                            files = event.files,
                        )
                    is GalleryViewModel.Event.ReturnDownloadedFile ->
                        returnDownloadedFile(
                            downloadedFile = event.downloadedFile,
                            mimeType = event.mimeType,
                            displayName = event.displayName,
                        )

                    is GalleryViewModel.Event.OpenViewer ->
                        openViewer(
                            mediaIndex = event.mediaIndex,
                            repositoryQuery = event.repositoryQuery,
                        )
                }

                log.debug {
                    "subscribeToEvents(): handled_new_event:" +
                            "\nevent=$event"
                }
            }
            .disposeOnDestroy(this)
    }

    private fun subscribeToState() {
        viewModel.state
            .observe(this) { state ->
                log.debug {
                    "subscribeToState(): received_new_state:" +
                            "\nstate=$state"
                }

                title = when (state) {
                    is GalleryViewModel.State.Selecting ->
                        if (state.filter != null)
                            getString(
                                R.string.template_select_media_type,
                                getString(GalleryMediaTypeResources.getName(state.filter))
                            )
                        else
                            getString(R.string.select_content)

                    GalleryViewModel.State.Viewing ->
                        getString(R.string.library)
                }

                log.debug {
                    "subscribeToState(): handled_new_state:" +
                            "\nstate=$state"
                }
            }
    }

    private fun subscribeToSearch() {
        val searchChipSpacing =
            resources.getDimensionPixelSize(R.dimen.gallery_search_media_type_chip_spacing)
        val searchChipContext = ContextThemeWrapper(
            this,
            R.style.MediaTypeChip
        )
        val searchChipLayoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            setMargins(0, 0, searchChipSpacing, searchChipSpacing)
        }

        with(view.searchContent.mediaTypeChipsLayout) {
            searchViewModel.availableMediaTypes.observe(this@GalleryActivity) { availableTypes ->
                availableTypes.forEach { mediaTypeName ->
                    addView(
                        Chip(searchChipContext).apply {
                            tag = mediaTypeName
                            setText(GalleryMediaTypeResources.getName(mediaTypeName))
                            setChipIconResource(GalleryMediaTypeResources.getIcon(mediaTypeName))
                            setEnsureMinTouchTargetSize(false)
                            isCheckable = true

                            setOnClickListener {
                                searchViewModel.onAvailableMediaTypeClicked(mediaTypeName)
                            }
                        },
                        searchChipLayoutParams,
                    )
                }
            }

            searchViewModel.selectedMediaTypes.observe(this@GalleryActivity) { selectedTypes ->
                forEach { chip ->
                    with(chip as Chip) {
                        if (selectedTypes.contains(tag)) {
                            isChecked = true
                            isCheckedIconVisible = true
                        } else {
                            isChecked = false
                            isCheckedIconVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun initList() {
        val galleryAdapter = FastAdapter.with(
            listOf(
                galleryItemsAdapter,
                galleryProgressFooterAdapter
            )
        ).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item, _ ->
                if (item is GalleryListItem) {
                    viewModel.onItemClicked(item)
                }
                false
            }
        }

        with(view.galleryRecyclerView) {
            setPadding(
                paddingLeft,
                paddingTop + resources.getDimensionPixelSize(R.dimen.list_item_gallery_media_margin),
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
                            R.id.list_item_gallery_progress,
                            R.id.list_item_gallery_header ->
                                spanCount

                            else ->
                                1
                        }
                }
            }

            adapter = galleryAdapter
            layoutManager = gridLayoutManager

            val endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener(
                footerAdapter = galleryProgressFooterAdapter,
                layoutManager = gridLayoutManager,
                visibleThreshold = gridLayoutManager.spanCount * 5
            ) {
                override fun onLoadMore(currentPage: Int) {
                    log.debug {
                        "onLoadMore(): load_more:" +
                                "\npage=$currentPage"
                    }
                    viewModel.loadMore()
                }
            }
            viewModel.isLoading.observe(this@GalleryActivity) { isLoading ->
                if (isLoading) {
                    endlessRecyclerOnScrollListener.disable()
                } else {
                    endlessRecyclerOnScrollListener.enable()
                }
            }
            addOnScrollListener(endlessRecyclerOnScrollListener)
        }
    }

    private fun initMediaFileSelection() {
        mediaFileSelectionView.init { fileItem ->
            if (fileItem.source != null) {
                viewModel.onFileSelected(fileItem.source)
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

    private fun returnDownloadedFile(
        downloadedFile: File,
        mimeType: String,
        displayName: String,
    ) {
        val resultIntent = fileReturnIntentCreator.createIntent(
            fileToReturn = downloadedFile,
            mimeType = mimeType,
            displayName = displayName,
        )
        setResult(Activity.RESULT_OK, resultIntent)

        log.debug {
            "returnDownloadedFile(): result_set_finishing:" +
                    "\nintent=$resultIntent," +
                    "\ndownloadedFile=$downloadedFile"
        }

        finish()
    }

    private fun openViewer(
        mediaIndex: Int,
        repositoryQuery: String?,
    ) {
        startActivity(
            Intent(this, MediaViewerActivity::class.java)
                .putExtras(
                    MediaViewerActivity.getBundle(
                        mediaIndex = mediaIndex,
                        repositoryQuery = repositoryQuery,
                    )
                )
        )
    }
}