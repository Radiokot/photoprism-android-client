package ua.com.radiokot.photoprism.features.prefs.view

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogBookmarksExportOptionsBinding

class BookmarksExportOptionsDialogFragment :
    BaseMaterialDialogFragment(R.layout.dialog_bookmarks_export_options) {

    private lateinit var viewBinding: DialogBookmarksExportOptionsBinding

    override fun onDialogViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        viewBinding = DialogBookmarksExportOptionsBinding.bind(dialogView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(
            viewBinding.saveButton,
            viewBinding.shareButton,
        ).forEach { button ->
            button.setOnClickListener {
                setFragmentResult(REQUEST_KEY, bundleOf(ID_KEY to button.id))
                dismiss()
            }
        }
    }

    companion object {
        private const val ID_KEY = "id"
        const val REQUEST_KEY = "bookmark-export-options"

        @IdRes
        fun getResult(bundle: Bundle): Int =
            bundle.getInt(ID_KEY, -1).also {
                check(it >= 0) {
                    "The result must contain the ID"
                }
            }
    }
}