package ua.com.radiokot.photoprism.features.people.view

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
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityPeopleSelectionBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.search.extension.bindToViewModel
import ua.com.radiokot.photoprism.features.gallery.search.extension.fixCloseButtonColor
import ua.com.radiokot.photoprism.features.gallery.search.extension.hideUnderline
import ua.com.radiokot.photoprism.features.people.view.model.PeopleSelectionViewModel
import ua.com.radiokot.photoprism.features.people.view.model.SelectablePersonListItem
import ua.com.radiokot.photoprism.view.ErrorView

class PeopleSelectionActivity : BaseActivity() {

    private val log = kLogger("PeopleSelectionActivity")
    private lateinit var view: ActivityPeopleSelectionBinding
    private val viewModel: PeopleSelectionViewModel by viewModel()
    private val adapter = ItemAdapter<SelectablePersonListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityPeopleSelectionBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.initOnce(
            currentlySelectedPersonIds =
            intent.getStringArrayExtra(SELECTED_PERSON_IDS_EXTRA)?.toSet(),
            currentlyNotSelectedPersonIds =
            intent.getStringArrayExtra(NOT_SELECTED_PERSON_IDS_EXTRA)?.toSet(),
        )

        // Init the list once it is laid out.
        view.peopleRecyclerView.doOnPreDraw {
            initList()
        }
        initErrorView()
        initSwipeRefresh()
        initButtons()

        subscribeToData()
        subscribeToEvents()

        // Allow the view model to intercept back press.
        onBackPressedDispatcher.addCallback(viewModel.backPressedCallback)
    }

    private fun initList() {
        val peopleAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: SelectablePersonListItem, _ ->
                viewModel.onPersonItemClicked(item)
                true
            }
        }

        with(view.peopleRecyclerView) {
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
                resources.getDimensionPixelSize(R.dimen.list_item_person_overview_min_width)
            val spanCount = (listWidth / minItemWidthPx).coerceAtLeast(1)

            log.debug {
                "initList(): calculated_grid:" +
                        "\nspanCount=$spanCount," +
                        "\nrowWidth=$listWidth," +
                        "\nminItemWidthPx=$minItemWidthPx"
            }

            adapter = peopleAdapter

            // Add the row spacing and make the items fill the column width
            // by overriding the layout manager layout params factory.
            layoutManager = object : GridLayoutManager(context, spanCount) {
                val rowSpacing: Int =
                    resources.getDimensionPixelSize(R.dimen.list_item_person_margin_end)

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
        view.errorView.replaces(view.peopleRecyclerView)
    }

    private fun initSwipeRefresh() = with(view.swipeRefreshLayout) {
        setOnRefreshListener(viewModel::onSwipeRefreshPulled)
    }

    private fun initButtons() {
        view.doneSelectingFab.setOnClickListener {
            viewModel.onDoneClicked()
        }
    }

    private fun subscribeToData() {
        viewModel.itemsList.observe(this, adapter::setNewList)

        viewModel.isLoading.observe(this, view.swipeRefreshLayout::setRefreshing)

        viewModel.mainError.observe(this) { mainError ->
            when (mainError) {
                PeopleSelectionViewModel.Error.LoadingFailed ->
                    view.errorView.showError(
                        ErrorView.Error.General(
                            context = view.errorView.context,
                            messageRes = R.string.failed_to_load_people,
                            retryButtonTextRes = R.string.try_again,
                            retryButtonClickListener = viewModel::onRetryClicked
                        )
                    )

                PeopleSelectionViewModel.Error.NothingFound ->
                    view.errorView.showError(
                        ErrorView.Error.EmptyView(
                            context = view.errorView.context,
                            messageRes = R.string.no_people_found,
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
            PeopleSelectionViewModel.Event.ShowFloatingLoadingFailedError ->
                showFloatingLoadingFailedError()

            is PeopleSelectionViewModel.Event.FinishWithResult ->
                finishWithResult(
                    selectedPersonIds = event.selectedPersonIds,
                    notSelectedPersonIds = event.notSelectedPersonIds,
                )

            is PeopleSelectionViewModel.Event.Finish ->
                finish()
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

    private fun showFloatingLoadingFailedError() {
        Snackbar.make(
            view.swipeRefreshLayout,
            getString(R.string.failed_to_load_people),
            Snackbar.LENGTH_SHORT
        )
            .setAction(R.string.try_again) { viewModel.onRetryClicked() }
            .show()
    }

    private fun finishWithResult(
        selectedPersonIds: Set<String>,
        notSelectedPersonIds: Set<String>,
    ) {
        log.debug {
            "finishWithResult(): finishing:" +
                    "\nselectedPeopleCount=${selectedPersonIds.size}," +
                    "\nnotSelectedPeopleCount=${notSelectedPersonIds.size}"
        }

        setResult(
            RESULT_OK,
            Intent().putExtras(
                createResult(
                    selectedPersonIds = selectedPersonIds,
                    notSelectedPersonIds = notSelectedPersonIds,
                )
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
            bindToViewModel(viewModel, this@PeopleSelectionActivity)
        }

        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val FALLBACK_LIST_SIZE = 100
        private const val SELECTED_PERSON_IDS_EXTRA = "selected_person_ids"
        private const val NOT_SELECTED_PERSON_IDS_EXTRA = "not_selected_person_ids"

        /**
         * @param selectedPersonIds if set, the initial selection will only contain given IDs
         * @param notSelectedPersonIds if set, the initial selection will contain all the IDs except given
         */
        fun getBundle(
            selectedPersonIds: Set<String>? = null,
            notSelectedPersonIds: Set<String>? = null,
        ) = Bundle().apply {
            require((selectedPersonIds == null) != (notSelectedPersonIds == null)) {
                "Either selected or not selected set of IDs must be set"
            }

            selectedPersonIds?.toTypedArray()?.also {
                putStringArray(SELECTED_PERSON_IDS_EXTRA, it)
            }
            notSelectedPersonIds?.toTypedArray()?.also {
                putStringArray(NOT_SELECTED_PERSON_IDS_EXTRA, it)
            }
        }

        private fun createResult(
            selectedPersonIds: Set<String>,
            notSelectedPersonIds: Set<String>,
        ) = Bundle().apply {
            putStringArray(SELECTED_PERSON_IDS_EXTRA, selectedPersonIds.toTypedArray())
            putStringArray(NOT_SELECTED_PERSON_IDS_EXTRA, notSelectedPersonIds.toTypedArray())
        }

        fun getSelectedPersonIds(bundle: Bundle): Set<String> =
            requireNotNull(bundle.getStringArray(SELECTED_PERSON_IDS_EXTRA)?.toSet()) {
                "No $SELECTED_PERSON_IDS_EXTRA specified"
            }

        fun getNotSelectedPersonIds(bundle: Bundle): Set<String> =
            requireNotNull(bundle.getStringArray(NOT_SELECTED_PERSON_IDS_EXTRA)?.toSet()) {
                "No $NOT_SELECTED_PERSON_IDS_EXTRA specified"
            }
    }
}
