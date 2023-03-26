package ua.com.radiokot.photoprism.features.gallery.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogSearchBookmarkBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.view.model.SearchBookmarkDialogViewModel

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

        val bookmark: SearchBookmark? = requireArguments().getParcelable(BOOKMARK_EXTRA)

        log.debug {
            "onViewCreated(): created:" +
                    "\nsavedInstanceState=$savedInstanceState," +
                    "\nbookmark=$bookmark"
        }

        initFields()
        initButtons()

        subscribeToState()
        subscribeToEvents()

        viewModel.initOnce(bookmark)
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
        viewBinding.saveButton.setOnClickListener {
            viewModel.onSaveButtonClicked()
        }

        viewBinding.deleteButton.setOnClickListener {
            viewModel.onDeleteButtonClicked()
        }

        viewBinding.closeButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    private fun subscribeToState() {
        viewModel.state.subscribeBy { state ->
            log.debug {
                "subscribeToState(): received_new_state:" +
                        "\nstate=$state"
            }

            viewBinding.deleteButton.isVisible =
                state is SearchBookmarkDialogViewModel.State.Editing

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.disposeOnDestroy(viewLifecycleOwner)
    }

    private fun subscribeToEvents() {
        viewModel.events.subscribe { event ->
            log.debug {
                "subscribeToEvents(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                SearchBookmarkDialogViewModel.Event.Dismiss ->
                    dismiss()
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.disposeOnDestroy(this)

    }

    companion object {
        private const val BOOKMARK_EXTRA = "bookmark"

        fun getBundle(bookmark: SearchBookmark?) = Bundle().apply {
            putParcelable(BOOKMARK_EXTRA, bookmark)
        }
    }
}