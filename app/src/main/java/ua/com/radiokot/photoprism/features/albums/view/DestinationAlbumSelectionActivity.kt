package ua.com.radiokot.photoprism.features.albums.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityDestinationAlbumSelectionBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setBetter
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.albums.data.model.DestinationAlbum
import ua.com.radiokot.photoprism.features.albums.view.model.DestinationAlbumListItem
import ua.com.radiokot.photoprism.features.albums.view.model.DestinationAlbumSelectionViewModel
import ua.com.radiokot.photoprism.view.ErrorView

class DestinationAlbumSelectionActivity : BaseActivity() {
    private val log = kLogger("DestinationAlbumSelectionActivity")

    private lateinit var view: ActivityDestinationAlbumSelectionBinding
    private val viewModel: DestinationAlbumSelectionViewModel by viewModel()
    private val adapter = ItemAdapter<DestinationAlbumListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityDestinationAlbumSelectionBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.initOnce(
            currentlySelectedAlbums = intent.extras
                ?.let(Companion::getSelectedAlbums)
                ?: emptySet()
        )

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

            onClickListener = { _, _, item: DestinationAlbumListItem, _ ->
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
        view.searchEditText.setOnEditorActionListener { _, _, _ ->
            viewModel.onSearchSubmit()

            // Do not close the keyboard.
            true
        }
    }

    private fun subscribeToData() {
        val diffCallback = DestinationAlbumListItemDiffCallback()
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
                DestinationAlbumSelectionViewModel.Error.LoadingFailed ->
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

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            DestinationAlbumSelectionViewModel.Event.ShowFloatingLoadingFailedError ->
                showFloatingLoadingFailedError()

            is DestinationAlbumSelectionViewModel.Event.Finish ->
                finish()

            is DestinationAlbumSelectionViewModel.Event.FinishWithResult ->
                finishWithResult(event.selectedAlbums)
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

    private fun finishWithResult(selectedAlbums: Set<DestinationAlbum>) {
        log.debug {
            "finishWithResult(): finishing:" +
                    "\nselectedAlbumCount=${selectedAlbums.size}"
        }

        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                createResult(selectedAlbums)
            )
        )
        finish()
    }

    private fun showFloatingLoadingFailedError() {
        Snackbar.make(
            view.swipeRefreshLayout,
            getString(R.string.failed_to_load_albums),
            Snackbar.LENGTH_SHORT
        )
            .setAction(R.string.try_again) { viewModel.onRetryClicked() }
            .show()
    }

    companion object {
        private const val SELECTED_ALBUMS_EXTRA = "selected_albums"

        fun getBundle(selectedAlbums: Set<DestinationAlbum>) = Bundle().apply {
            putParcelableArrayList(SELECTED_ALBUMS_EXTRA, ArrayList(selectedAlbums))
        }

        private fun createResult(selectedAlbums: Set<DestinationAlbum>) =
            getBundle(selectedAlbums)

        @Suppress("DEPRECATION")
        fun getSelectedAlbums(bundle: Bundle): Set<DestinationAlbum> =
            requireNotNull(
                bundle.getParcelableArrayList<DestinationAlbum>(SELECTED_ALBUMS_EXTRA)?.toSet()
            ) {
                "No $SELECTED_ALBUMS_EXTRA specified"
            }
    }
}
