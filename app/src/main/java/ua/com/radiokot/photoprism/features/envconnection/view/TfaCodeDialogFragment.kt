package ua.com.radiokot.photoprism.features.envconnection.view

import android.app.Dialog
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogTfaCodeBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.util.SoftInputVisibility

class TfaCodeDialogFragment : BaseMaterialDialogFragment(R.layout.dialog_tfa_code) {
    private lateinit var viewBinding: DialogTfaCodeBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    override fun onDialogViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        viewBinding = DialogTfaCodeBinding.bind(dialogView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initFields()
        initButtons()
    }

    private fun initFields() {
        with(viewBinding.codeTextInput.editText!!) {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    dismissWithResult()
                    true
                } else {
                    false
                }
            }

            requestFocus()
        }
    }

    private fun initButtons() {
        viewBinding.continueButton.setThrottleOnClickListener {
            dismissWithResult()
        }

        viewBinding.closeButton.setThrottleOnClickListener {
            dismiss()
        }

        viewBinding.codeTextInput.setEndIconOnClickListener {
            requireContext().getSystemService<ClipboardManager>()
                ?.primaryClip
                ?.getItemAt(0)
                ?.text
                ?.toString()
                ?.also { clipboardText ->
                    viewBinding.codeTextInput.editText!!.setText(clipboardText)
                    dismissWithResult()
                }
        }
    }

    private fun dismissWithResult() {
        val filteredInput = viewBinding.codeTextInput.editText!!
            .text
            .toString()
            .trim()
            .takeIf(String::isNotBlank)
            ?: return

        setFragmentResult(REQUEST_KEY, bundleOf(CODE_EXTRA to filteredInput))
        SoftInputVisibility.hide(dialog?.window!!)
        dismiss()
    }

    companion object {
        private const val CODE_EXTRA = "code"
        const val REQUEST_KEY = "tfa-code"
        const val TAG = "tfa-dialog"

        fun getResult(bundle: Bundle): String =
            bundle.getString(CODE_EXTRA, null).checkNotNull {
                "The result must contain the entered code"
            }
    }
}
