package ua.com.radiokot.photoprism.features.gallery.view

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogSearchBookmarkBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.view.model.SearchBookmarkDialogViewModel

class SearchBookmarkDialogFragment : BaseMaterialDialogFragment(R.layout.dialog_search_bookmark) {
    private val log = kLogger("SearchBookmarkDialog")

    private lateinit var viewBinding: DialogSearchBookmarkBinding
    private val viewModel: SearchBookmarkDialogViewModel by viewModel()

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

        viewModel.isDeleteButtonVisible.observe(
            viewLifecycleOwner,
            viewBinding.deleteButton::isVisible::set
        )

        viewBinding.closeButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    companion object {
        private const val BOOKMARK_EXTRA = "bookmark"

        fun getBundle(bookmark: SearchBookmark?) = Bundle().apply {
            putParcelable(BOOKMARK_EXTRA, bookmark)
        }
    }
}