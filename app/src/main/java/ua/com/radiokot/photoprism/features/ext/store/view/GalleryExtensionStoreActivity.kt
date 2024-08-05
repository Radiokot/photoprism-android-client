package ua.com.radiokot.photoprism.features.ext.store.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.addClickListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityExtensionStoreBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.ext.key.activation.view.KeyActivationActivity
import ua.com.radiokot.photoprism.features.ext.store.view.model.GalleryExtensionStoreDisclaimerListItem
import ua.com.radiokot.photoprism.features.ext.store.view.model.GalleryExtensionStoreListItem
import ua.com.radiokot.photoprism.features.ext.store.view.model.GalleryExtensionStoreViewModel
import ua.com.radiokot.photoprism.util.SafeCustomTabs
import ua.com.radiokot.photoprism.view.ErrorView

class GalleryExtensionStoreActivity : BaseActivity() {
    private val log = kLogger("GalleryExtensionStoreActivity")

    private lateinit var view: ActivityExtensionStoreBinding
    private val viewModel: GalleryExtensionStoreViewModel by viewModel()
    private val adapter = ItemAdapter<GalleryExtensionStoreListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityExtensionStoreBinding.inflate(layoutInflater)
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
        val disclaimerAdapter = ItemAdapter<GalleryExtensionStoreDisclaimerListItem>().apply {
            setNewList(listOf(GalleryExtensionStoreDisclaimerListItem))
        }

        val itemsAdapter = FastAdapter.with(listOf(disclaimerAdapter, adapter)).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            addClickListener(
                resolveView = { viewHolder: RecyclerView.ViewHolder ->
                    when (viewHolder) {
                        is GalleryExtensionStoreListItem.ViewHolder ->
                            viewHolder.view.buyButton

                        is GalleryExtensionStoreDisclaimerListItem.ViewHolder ->
                            viewHolder.view.gotItButton

                        else ->
                            null
                    }
                },
                resolveViews = { null },
                onClick = { _, _, _, item ->
                    when (item) {
                        is GalleryExtensionStoreListItem ->
                            viewModel.onBuyNowClicked(item)

                        is GalleryExtensionStoreDisclaimerListItem ->
                            TODO("Implement hiding the disclaimer")
                    }
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
                GalleryExtensionStoreViewModel.Error.LoadingFailed ->
                    view.errorView.showError(
                        ErrorView.Error.General(
                            context = view.errorView.context,
                            messageRes = R.string.extension_store_failed_loading,
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
            GalleryExtensionStoreViewModel.Event.ShowFloatingLoadingFailedError ->
                showFloatingLoadingFailedError()

            is GalleryExtensionStoreViewModel.Event.OpenOnlinePurchase ->
                openOnlinePurchase(
                    url = event.url,
                )

            GalleryExtensionStoreViewModel.Event.OpenKeyActivation ->
                startActivity(Intent(this, KeyActivationActivity::class.java))
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }.autoDispose(this)

    private fun showFloatingLoadingFailedError() {
        Snackbar.make(
            view.swipeRefreshLayout,
            getString(R.string.extension_store_failed_loading),
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
            titleRes = R.string.extension_store_title,
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.extension_store, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.activate_key) {
            viewModel.onActivateKeyClicked()
        }

        return super.onOptionsItemSelected(item)
    }
}
