package ua.com.radiokot.photoprism.features.gallery.view

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
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
import ua.com.radiokot.photoprism.databinding.ActivityGalleryBinding
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryProgressListItem
import java.io.File


class GalleryActivity : AppCompatActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            linkTo(getScope("session"))
        }
    }

    private lateinit var view: ActivityGalleryBinding
    private val viewModel: GalleryViewModel by viewModel()
    private val log = kLogger("GGalleryActivity")

    private val galleryItemsAdapter = ItemAdapter<GalleryMediaListItem>()
    private val galleryProgressFooterAdapter = ItemAdapter<GalleryProgressListItem>()

    private val fileReturnIntentCreator: FileReturnIntentCreator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.debug {
            "onCreate(): creating:" +
                    "\naction=${intent.action}," +
                    "\nextras=${intent.extras}," +
                    "\ntype=${intent.type}" +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        view = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(view.root)

        subscribeToViewModel()

        view.galleryRecyclerView.post(::initList)
        initMediaFileSelection()
    }

    private fun subscribeToViewModel() {
        viewModel.isLoading
            .observe(this) { isLoading ->
                view.isLoadingTextView.text = isLoading.toString()
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

        viewModel.events
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                log.debug {
                    "subscribeToViewModel(): received_new_event:" +
                            "\nevent=$event"
                }

                when (event) {
                    is GalleryViewModel.Event.OpenFileSelectionDialog ->
                        openMediaFilesDialog(event.files)
                    is GalleryViewModel.Event.ShowDownloadProgress ->
                        showDownloadProgress(event.percent)
                    is GalleryViewModel.Event.DismissDownloadProgress ->
                        dismissDownloadProgress()
                    is GalleryViewModel.Event.ShowDownloadError ->
                        showDownloadError()
                    is GalleryViewModel.Event.ReturnDownloadedFile ->
                        returnDownloadedFile(
                            downloadedFile = event.downloadedFile,
                            mimeType = event.mimeType,
                            displayName = event.displayName,
                        )
                }

                log.debug {
                    "subscribeToViewModel(): handled_new_event:" +
                            "\nevent=$event"
                }
            }
            .disposeOnDestroy(this)
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
                if (item is GalleryMediaListItem) {
                    viewModel.onItemClicked(item)
                }
                false
            }
        }

        with(view.galleryRecyclerView) {
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

            val gridLayoutManager = GridLayoutManager(context, spanCount).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int =
                        if (galleryAdapter.getItemViewType(position) == R.id.list_item_gallery_progress)
                            spanCount
                        else
                            1
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
        supportFragmentManager.setFragmentResultListener(
            MediaFilesDialogFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            val selectedFile = MediaFilesDialogFragment.getResult(bundle)

            log.debug {
                "onFragmentResult(): got_selected_media_file:" +
                        "\nfile=$selectedFile"
            }

            viewModel.onFileSelected(selectedFile)
        }
    }

    private fun openMediaFilesDialog(files: List<GalleryMedia.File>) {
        MediaFilesDialogFragment()
            .apply {
                arguments = MediaFilesDialogFragment.getBundle(files)
            }
            .show(supportFragmentManager, "media-files")
    }

    private fun showDownloadProgress(percent: Double) {
        val fragment =
            (supportFragmentManager.findFragmentByTag(DOWNLOAD_PROGRESS_DIALOG_TAG) as? DownloadProgressDialogFragment)
                ?: DownloadProgressDialogFragment().apply {
                    cancellationEvent.observe(this@GalleryActivity) {
                        viewModel.onDownloadProgressDialogCancelled()
                    }
                }

        if (!fragment.isAdded || !fragment.showsDialog) {
            fragment.showNow(supportFragmentManager, DOWNLOAD_PROGRESS_DIALOG_TAG)
        }

        fragment.setProgress(percent)
    }

    private fun dismissDownloadProgress() {
        (supportFragmentManager.findFragmentByTag(DOWNLOAD_PROGRESS_DIALOG_TAG) as? DialogFragment)
            ?.dismiss()
    }

    private fun showDownloadError() {
        Snackbar.make(
            view.galleryRecyclerView,
            R.string.failed_to_download_file,
            Snackbar.LENGTH_SHORT
        )
            .show()
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

    private companion object {
        private const val DOWNLOAD_PROGRESS_DIALOG_TAG = "download-progress"
    }
}