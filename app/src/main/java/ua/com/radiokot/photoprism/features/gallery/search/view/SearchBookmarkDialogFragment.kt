package ua.com.radiokot.photoprism.features.gallery.search.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogSearchBookmarkBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.search.view.model.SearchBookmarkDialogViewModel

class SearchBookmarkDialogFragment : BaseMaterialDialogFragment(R.layout.dialog_search_bookmark) {
    private val log = kLogger("SearchBookmarkDialog")

    private lateinit var viewBinding: DialogSearchBookmarkBinding
    private val viewModel: SearchBookmarkDialogViewModel by viewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    override fun onDialogViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        viewBinding = DialogSearchBookmarkBinding.bind(dialogView)
    }

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val existingBookmark: SearchBookmark? = requireArguments().getParcelable(BOOKMARK_EXTRA)
        val searchConfig: SearchConfig =
            requireNotNull(requireArguments().getParcelable(SEARCH_CONFIG_EXTRA)) {
                "No search config specified"
            }

        log.debug {
            "onViewCreated(): created:" +
                    "\nsavedInstanceState=$savedInstanceState," +
                    "\nexistingBookmark=$existingBookmark," +
                    "\nsearchConfig=$searchConfig"
        }

        initFields()
        initButtons()

        subscribeToState()
        subscribeToEvents()

        viewModel.initOnce(
            searchConfig = searchConfig,
            existingBookmark = existingBookmark,
        )
    }

    private fun initFields() {
        with(viewBinding.nameTextInput) {
            with(editText!!) {
                bindTextTwoWay(viewModel.name, viewLifecycleOwner)
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        viewModel.onNameSubmitted()
                    } else {
                        false
                    }
                }

                requestFocus()
            }

            isCounterEnabled = true
            counterMaxLength = viewModel.nameMaxLength
        }
    }

    private fun initButtons() {
        viewModel.isSaveButtonEnabled.observe(
            viewLifecycleOwner,
            viewBinding.saveButton::setEnabled
        )
        viewBinding.saveButton.setThrottleOnClickListener {
            viewModel.onSaveButtonClicked()
        }

        viewBinding.deleteButton.setThrottleOnClickListener {
            viewModel.onDeleteButtonClicked()
        }

        viewBinding.closeButton.setThrottleOnClickListener {
            dialog?.cancel()
        }
    }

    private fun subscribeToState() = viewModel.state.subscribe(this) { state ->
        log.debug {
            "subscribeToState(): received_new_state:" +
                    "\nstate=$state"
        }

        with(viewBinding.deleteButton) {
            val wasVisible = isVisible
            isVisible = state is SearchBookmarkDialogViewModel.State.Editing

            // Do not enable "Delete" immediately to avoid missclick.
            if (!wasVisible && isVisible) {
                isEnabled = false
                postDelayed({
                    isEnabled = true
                }, 1000)
            }
        }

        viewBinding.titleTextView.text = when (state) {
            is SearchBookmarkDialogViewModel.State.Creating ->
                getString(R.string.add_search_bookmark)

            is SearchBookmarkDialogViewModel.State.Editing ->
                getString(R.string.edit_search_bookmark)
        }

        log.debug {
            "subscribeToState(): handled_new_state:" +
                    "\nstate=$state"
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            SearchBookmarkDialogViewModel.Event.Dismiss ->
                dismiss()

            is SearchBookmarkDialogViewModel.Event.ShowFloatingError ->
                Toast.makeText(
                    requireContext(),
                    getString(
                        when (event.error) {
                            is SearchBookmarkDialogViewModel.Error.FailedToCreate ->
                                R.string.template_error_failed_to_add_bookmark

                            is SearchBookmarkDialogViewModel.Error.FailedToDelete ->
                                R.string.template_error_failed_to_delete_bookmark

                            is SearchBookmarkDialogViewModel.Error.FailedToUpdate ->
                                R.string.template_error_failed_to_edit_bookmark
                        },
                        event.error.shortSummary,
                    ),
                    Toast.LENGTH_LONG
                ).show()
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

    companion object {
        private const val BOOKMARK_EXTRA = "bookmark"
        private const val SEARCH_CONFIG_EXTRA = "search-config"

        fun getBundle(
            searchConfig: SearchConfig,
            existingBookmark: SearchBookmark?,
        ) = Bundle().apply {
            putParcelable(BOOKMARK_EXTRA, existingBookmark)
            putParcelable(SEARCH_CONFIG_EXTRA, searchConfig)
        }
    }
}
