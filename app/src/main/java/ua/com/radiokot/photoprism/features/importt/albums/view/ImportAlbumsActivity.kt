package ua.com.radiokot.photoprism.features.importt.albums.view

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityImportAlbumsBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setBetter
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.importt.albums.view.model.ImportAlbumListItem
import ua.com.radiokot.photoprism.features.importt.albums.view.model.ImportAlbumsViewModel
import ua.com.radiokot.photoprism.view.ErrorView

class ImportAlbumsActivity : BaseActivity() {
    private val log = kLogger("ImportAlbumsActivity")

    private lateinit var view: ActivityImportAlbumsBinding
    private val viewModel: ImportAlbumsViewModel by viewModel()
    private val adapter = ItemAdapter<ImportAlbumListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityImportAlbumsBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initList()
        initSwipeRefresh()
        initButtons()
        initSearch()

        subscribeToData()
        subscribeToEvents()

        // Allow the view model to intercept back press.
        onBackPressedDispatcher.addCallback(viewModel.backPressedCallback)
    }

    private fun initList() {
        val itemsAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: ImportAlbumListItem, _ ->
                viewModel.onListItemClicked(item)
                true
            }
        }

        view.albumsRecyclerView.adapter = itemsAdapter
    }

    private fun initSwipeRefresh() = with(view.swipeRefreshLayout) {
        setOnRefreshListener(viewModel::onSwipeRefreshPulled)
    }

    private fun initButtons() {
        view.doneSelectingFab.setThrottleOnClickListener {
            viewModel.onDoneClicked()
        }
    }

    private fun initSearch() {
        view.searchEditText.bindTextTwoWay(viewModel.rawSearchInput, this)
    }

    private fun subscribeToData() {
        val diffCallback = ImportAlbumListItemDiffCallback()
        viewModel.itemsList.observe(this) { newItems ->
            FastAdapterDiffUtil.setBetter(
                recyclerView = view.albumsRecyclerView,
                adapter = adapter,
                items = newItems,
                callback = diffCallback,
                detectMoves = false,
            )
        }

        viewModel.isLoading.observe(this, view.swipeRefreshLayout::setRefreshing)

        viewModel.mainError.observe(this) { mainError ->
            when (mainError) {
                ImportAlbumsViewModel.Error.LoadingFailed ->
                    view.errorView.showError(
                        ErrorView.Error.General(
                            context = view.errorView.context,
                            messageRes = R.string.failed_to_load_albums,
                            retryButtonTextRes = R.string.try_again,
                            retryButtonClickListener = viewModel::onRetryClicked
                        )
                    )

                null ->
                    view.errorView.hide()
            }
        }

        viewModel.isDoneButtonVisible.observe(this) { isDoneButtonVisible ->
            if (isDoneButtonVisible) {
                view.doneSelectingFab.show()
            } else {
                view.doneSelectingFab.hide()
            }
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            ImportAlbumsViewModel.Event.ShowFloatingLoadingFailedError ->
                showFloatingLoadingFailedError()

            is ImportAlbumsViewModel.Event.Finish ->
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
}
