package ua.com.radiokot.photoprism.features.labels.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
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
import ua.com.radiokot.photoprism.databinding.ActivityLabelsBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.proxyOkResult
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.search.extension.bindToViewModel
import ua.com.radiokot.photoprism.features.gallery.search.extension.fixCloseButtonColor
import ua.com.radiokot.photoprism.features.gallery.search.extension.hideUnderline
import ua.com.radiokot.photoprism.features.gallery.view.GallerySingleRepositoryActivity
import ua.com.radiokot.photoprism.features.labels.view.model.LabelListItem
import ua.com.radiokot.photoprism.features.labels.view.model.LabelsViewModel
import ua.com.radiokot.photoprism.view.ErrorView

class LabelsActivity : BaseActivity() {

    private val log = kLogger("LabelsActivity")
    private lateinit var view: ActivityLabelsBinding
    private val viewModel: LabelsViewModel by viewModel()
    private val labelLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::proxyOkResult,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityLabelsBinding.inflate(layoutInflater)
        setContentView(view.root)

        @Suppress("DEPRECATION")
        viewModel.initOnce(
            defaultSearchConfig = intent.getParcelableExtra(DEFAULT_SEARCH_CONFIG_EXTRA)!!,
        )

        initToolbar()
        // Init the list once it is laid out.
        view.labelsRecyclerView.doOnPreDraw {
            initList()
        }
        initErrorView()
        initSwipeRefresh()

        subscribeToEvents()

        // Allow the view model to intercept back press.
        onBackPressedDispatcher.addCallback(viewModel.backPressedCallback)
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    private fun initList() {
        val labelsAdapter = ItemAdapter<LabelListItem>()

        viewModel.itemsList.observe(this, labelsAdapter::setNewList)

        with(view.labelsRecyclerView) {
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
                resources.getDimensionPixelSize(R.dimen.list_item_label_width)
            val spanCount = (listWidth / minItemWidthPx).coerceAtLeast(1)

            log.debug {
                "initList(): calculated_grid:" +
                        "\nspanCount=$spanCount," +
                        "\nrowWidth=$listWidth," +
                        "\nminItemWidthPx=$minItemWidthPx"
            }

            adapter = FastAdapter.with(labelsAdapter).apply {
                stateRestorationPolicy =
                    androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

                onClickListener = { _, _, item: LabelListItem, _ ->
                    viewModel.onLabelItemClicked(item)
                    true
                }
            }

            // Add the row spacing and make the items fill the column width
            // by overriding the layout manager layout params factory.
            layoutManager = object : GridLayoutManager(context, spanCount) {
                val rowSpacing: Int =
                    resources.getDimensionPixelSize(R.dimen.list_item_label_margin_end)

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
                    androidx.core.content.ContextCompat.getDrawable(
                        context,
                        R.drawable.fast_scroll_track
                    )!!
                )
                .setThumbDrawable(
                    androidx.core.content.ContextCompat.getDrawable(
                        context,
                        R.drawable.fast_scroll_thumb
                    )!!
                )
                .build()
        }
    }

    private fun initErrorView() {
        view.errorView.replaces(view.labelsRecyclerView)
        viewModel.mainError.observe(this) { mainError ->
            when (mainError) {
                LabelsViewModel.Error.LoadingFailed ->
                    view.errorView.showError(
                        ErrorView.Error.General(
                            context = view.errorView.context,
                            messageRes = R.string.failed_to_load_labels,
                            retryButtonTextRes = R.string.try_again,
                            retryButtonClickListener = viewModel::onRetryClicked
                        )
                    )

                LabelsViewModel.Error.NothingFound ->
                    view.errorView.showError(
                        ErrorView.Error.EmptyView(
                            context = view.errorView.context,
                            messageRes = R.string.no_labels_found,
                        )
                    )

                null ->
                    view.errorView.hide()
            }
        }
    }

    private fun initSwipeRefresh() = with(view.swipeRefreshLayout) {
        setOnRefreshListener(viewModel::onSwipeRefreshPulled)
        viewModel.isLoading.observe(this@LabelsActivity, ::setRefreshing)
    }

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            LabelsViewModel.Event.ShowFloatingLoadingFailedError ->
                showFloatingLoadingFailedError()

            is LabelsViewModel.Event.Finish ->
                finish()

            is LabelsViewModel.Event.OpenLabel ->
                openLabel(
                    name = event.name,
                    repositoryParams = event.repositoryParams,
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
            R.string.failed_to_load_labels,
            Snackbar.LENGTH_SHORT
        )
            .setAction(R.string.try_again) { viewModel.onRetryClicked() }
            .show()
    }

    private fun openLabel(
        name: String,
        repositoryParams: SimpleGalleryMediaRepository.Params,
    ) = labelLauncher.launch(
        Intent(this, GallerySingleRepositoryActivity::class.java)
            .setAction(intent.action)
            .putExtras(intent.extras ?: Bundle())
            .putExtras(
                GallerySingleRepositoryActivity.getBundle(
                    title = name,
                    repositoryParams = repositoryParams,
                )
            )
    )

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.labels, menu)

        // Set up the search.
        with(menu?.findItem(R.id.search_view)?.actionView as SearchView) {
            queryHint = getString(R.string.enter_the_query)
            fixCloseButtonColor()
            hideUnderline()
            bindToViewModel(viewModel, this@LabelsActivity)
        }

        with(menu.findItem(R.id.show_all)) {
            viewModel.isShowingAllLabels.observe(this@LabelsActivity) { isShowingAllLabels ->
                if (isShowingAllLabels) {
                    setTitle(R.string.show_only_important_labels)
                    setIcon(R.drawable.ic_eye_off)
                } else {
                    setTitle(R.string.show_all_labels)
                    setIcon(R.drawable.ic_eye)
                }
            }

            setOnMenuItemClickListener {
                viewModel.onShowAllClicked()
                true
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val FALLBACK_LIST_SIZE = 100
        private const val DEFAULT_SEARCH_CONFIG_EXTRA = "default_search_config"

        /**
         * @param defaultSearchConfig [SearchConfig] to be used as a base for opening a label.
         * It could, for example, include limited media types.
         */
        fun getBundle(
            defaultSearchConfig: SearchConfig,
        ) = Bundle().apply {
            putParcelable(DEFAULT_SEARCH_CONFIG_EXTRA, defaultSearchConfig)
        }
    }
}
