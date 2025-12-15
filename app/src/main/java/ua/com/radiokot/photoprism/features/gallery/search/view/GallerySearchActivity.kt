package ua.com.radiokot.photoprism.features.gallery.search.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityGallerySearchBinding
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchConfigBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel

/**
 * A standalone activity for configuring gallery search on Android TV.
 * This prevents D-pad interference with gallery content in the background.
 */
class GallerySearchActivity : BaseActivity() {
    private val log = kLogger("GallerySearchActivity")

    private lateinit var view: ActivityGallerySearchBinding
    private val viewModel: GallerySearchViewModel by viewModel()
    private lateinit var searchConfigView: GallerySearchConfigView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityGallerySearchBinding.inflate(layoutInflater)
        setContentView(view.root)

        // Start in configuring state
        @Suppress("DEPRECATION")
        intent
            .getParcelableExtra<SearchConfig?>(SEARCH_CONFIG_EXTRA)
            ?.also(viewModel::applySearchConfig)
        viewModel.switchToConfiguring()

        initToolbar()
        initTextSearchButton()
        initSearchConfigView()
        subscribeToState()
        subscribeToUserQuery()

        setResult(Activity.RESULT_CANCELED)
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.search_the_library)
    }

    private fun initTextSearchButton() {
        view.textSearchButton.setOnClickListener {
            showTextSearchDialog()
        }

        view.clearTextButton.setOnClickListener {
            viewModel.userQuery.value = ""
        }
    }

    private fun showTextSearchDialog() {
//        val dialogView = layoutInflater.inflate(
//            R.layout.dialog_text_input,
//            null
//        )
//        val editText = dialogView.findViewById<TextInputEditText>(R.id.text_input_edit_text)
//        editText.setText(viewModel.userQuery.value ?: "")
//
//        AlertDialog.Builder(this)
//            .setTitle(R.string.enter_the_query)
//            .setView(dialogView)
//            .setPositiveButton(android.R.string.ok) { _, _ ->
//                viewModel.userQuery.value = editText.text?.toString() ?: ""
//            }
//            .setNegativeButton(android.R.string.cancel, null)
//            .show()
//            .also {
//                // Request focus and show keyboard
//                editText.requestFocus()
//                editText.postDelayed({
//                    editText.requestFocus()
//                }, 100)
//            }
    }

    private fun subscribeToUserQuery() {
        viewModel.userQuery.observe(this) { query ->
            val hasQuery = !query.isNullOrBlank()

            // Update button text to show current query or placeholder
            view.textSearchButton.text = if (hasQuery) {
                query
            } else {
                getString(R.string.enter_the_query)
            }

            // Show/hide clear button
            view.clearTextButton.visibility = if (hasQuery) View.VISIBLE else View.GONE
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
