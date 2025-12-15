package ua.com.radiokot.photoprism.features.gallery.search.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityGallerySearchConfigBinding
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchConfigBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel

/**
 * A standalone activity for configuring gallery search on Android TV.
 * This prevents D-pad interference with gallery content in the background.
 */
class GallerySearchConfigActivity : BaseActivity() {
    private val log = kLogger("GallerySearchConfigActivity")

    private lateinit var view: ActivityGallerySearchConfigBinding
    private val viewModel: GallerySearchViewModel by viewModel()
    private lateinit var searchConfigView: GallerySearchConfigView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityGallerySearchConfigBinding.inflate(layoutInflater)
        setContentView(view.root)

        // Start in configuring state
        @Suppress("DEPRECATION")
        intent
            .getParcelableExtra<SearchConfig?>(SEARCH_CONFIG_EXTRA)
            ?.also(viewModel::applySearchConfig)
        viewModel.switchToConfiguring()

        initToolbar()
        initQueryInput()
        initSearchConfigView()
        subscribeToState()

        setResult(Activity.RESULT_CANCELED)
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.search_the_library)
    }

    private fun initQueryInput() {
        view.queryTextInput.editText!!.bindTextTwoWay(viewModel.userQuery, this)
        view.queryTextInput.editText!!.setOnEditorActionListener { _, _, _ ->
            viewModel.onSearchClicked()

            // Do not close the keyboard.
            true
        }
    }

    private fun initSearchConfigView() {
        val configBinding = ViewGallerySearchConfigBinding.bind(view.searchContent.root)

        searchConfigView = GallerySearchConfigView(
            view = configBinding,
            viewModel = viewModel,
            activity = this,
        )

        searchConfigView.initOnce()
    }

    private fun subscribeToState() {
        viewModel.state.subscribe(this) { state ->
            log.debug {
                "subscribeToState(): received_state:" +
                        "\nstate=$state"
            }

            when (state) {
                is GallerySearchViewModel.State.Applied -> {
                    // Search was applied,
                    // return the config to the calling activity.
                    setResult(
                        Activity.RESULT_OK,
                        Intent()
                            .putExtra(SEARCH_CONFIG_EXTRA, state.search.config)
                    )
                    finish()
                }

                is GallerySearchViewModel.State.NoSearch -> {
                    // Search was reset,
                    // return to the calling activity without applied config.
                    setResult(Activity.RESULT_OK, null)
                    finish()
                }

                is GallerySearchViewModel.State.Configuring -> {
                    // User is configuring search, stay on this screen
                }
            }
        }
    }

    companion object {
        private const val SEARCH_CONFIG_EXTRA = "search-config"

        fun getBundle(
            alreadyAppliedSearchConfig: SearchConfig?,
        ) = Bundle().apply {
            putParcelable(SEARCH_CONFIG_EXTRA, alreadyAppliedSearchConfig)
        }

        @Suppress("DEPRECATION")
        fun getResult(
            okResultIntent: Intent?,
        ): SearchConfig? =
            okResultIntent?.getParcelableExtra(SEARCH_CONFIG_EXTRA)
    }
}
