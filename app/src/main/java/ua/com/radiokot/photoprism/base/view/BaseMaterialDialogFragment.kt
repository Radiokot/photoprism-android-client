package ua.com.radiokot.photoprism.base.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ua.com.radiokot.photoprism.extension.checkNotNull

abstract class BaseMaterialDialogFragment(contentLayoutId: Int) : DialogFragment(contentLayoutId) {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = onCreateView(layoutInflater, null, savedInstanceState)
            .checkNotNull()

        onDialogViewCreated(view, savedInstanceState)

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(view)
            .create()
    }

    open fun onDialogViewCreated(
        dialogView: View,
        savedInstanceState: Bundle?,
    ) {
    }
}