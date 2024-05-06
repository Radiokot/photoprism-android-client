package ua.com.radiokot.photoprism.features.ext.marketplace.view

import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.addClickListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityExtensionMarketplaceBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.ext.marketplace.view.model.GalleryExtensionMarketplaceListItem
import ua.com.radiokot.photoprism.features.ext.marketplace.view.model.GalleryExtensionMarketplaceViewModel
import ua.com.radiokot.photoprism.util.SafeCustomTabs
import ua.com.radiokot.photoprism.view.ErrorView

class GalleryExtensionMarketplaceActivity : BaseActivity() {
    private val log = kLogger("GalleryExtensionMarketplaceActivity")

    private lateinit var view: ActivityExtensionMarketplaceBinding
    private val viewModel: GalleryExtensionMarketplaceViewModel by viewModel()
    private val adapter = ItemAdapter<GalleryExtensionMarketplaceListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityExtensionMarketplaceBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initList()
        initErrorView()
        initSwipeRefresh()
        initCustomTabs()

        subscribeToData()
        subscribeToEvents()
    }

    private fun initList() {
        val itemsAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            addClickListener(
                resolveView = { viewHolder: GalleryExtensionMarketplaceListItem.ViewHolder ->
                    viewHolder.view.buyButton
                },
                resolveViews = { null },
                onClick = { _, _, _, item ->
                    viewModel.onBuyNowClicked(item)
                }
            )
        }

        view.itemsRecyclerView.adapter = itemsAdapter
    }

    private fun initErrorView() {
        view.errorView.replaces(view.itemsRecyclerView)
    }

    private fun initSwipeRefresh() = with(view.swipeRefreshLayout) {
        setOnRefreshListener(viewModel::onSwipeRefreshPulled)
    }

    private fun initCustomTabs() {
        SafeCustomTabs.safelyConnectAndInitialize(this)
    }

    private fun subscribeToData() {
        viewModel.itemsList.observe(this, adapter::setNewList)

        viewModel.isLoading.observe(this, view.swipeRefreshLayout::setRefreshing)

        viewModel.mainError.observe(this) { mainError ->
            when (mainError) {
                GalleryExtensionMarketplaceViewModel.Error.LoadingFailed ->
                    view.errorView.showError(
                        ErrorView.Error.General(
                            context = view.errorView.context,
                            messageRes = R.string.extension_marketplace_failed_loading,
                            retryButtonTextRes = R.string.try_again,
                            retryButtonClickListener = viewModel::onRetryClicked
                        )
                    )

                null ->
                    view.errorView.hide()
            }
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            GalleryExtensionMarketplaceViewModel.Event.ShowFloatingLoadingFailedError ->
                showFloatingLoadingFailedError()

            is GalleryExtensionMarketplaceViewModel.Event.OpenOnlinePurchase ->
                openOnlinePurchase(
                    url = event.url,
                )
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }.autoDispose(this)

    private fun showFloatingLoadingFailedError() {
        Snackbar.make(
            view.swipeRefreshLayout,
            getString(R.string.extension_marketplace_failed_loading),
            Snackbar.LENGTH_SHORT
        )
            .setAction(R.string.try_again) { viewModel.onRetryClicked() }
            .show()
    }

    private fun openOnlinePurchase(url: String) {
        SafeCustomTabs.launchWithFallback(
            context = this,
            intent = CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setUrlBarHidingEnabled(true)
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .build(),
            url = url,
            titleRes = R.string.extension_marketplace_title,
        )
    }
}
