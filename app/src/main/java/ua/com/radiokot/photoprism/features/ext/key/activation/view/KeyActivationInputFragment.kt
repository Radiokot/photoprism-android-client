package ua.com.radiokot.photoprism.features.ext.key.activation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import ua.com.radiokot.photoprism.databinding.FragmentKeyActivationInputBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.ext.key.activation.view.model.KeyActivationViewModel

class KeyActivationInputFragment : Fragment() {
    private lateinit var view: FragmentKeyActivationInputBinding
    private val viewModel: KeyActivationViewModel
        get() = (requireActivity() as KeyActivationActivity).viewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = FragmentKeyActivationInputBinding.inflate(inflater)
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initFields()
        initButtons()
    }

    private fun initFields() {
        with(view.keyTextInput) {
            with(editText!!) {
                // Only works if set programmatically on the EditText 🤷.
                maxLines = 3
                setHorizontallyScrolling(false)

                bindTextTwoWay(viewModel.key, viewLifecycleOwner)

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        viewModel.onKeyInputSubmit()
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    private fun initButtons() {
        with(view.continueButton) {
            viewModel.canSubmitKeyInput.observe(
                viewLifecycleOwner,
                this::setEnabled
            )

            setThrottleOnClickListener {
                viewModel.onKeyInputSubmit()
            }
        }

        view.keyTextInput.setEndIconOnClickListener {
            viewModel.onKeyInputPasteClicked()
        }
    }
}
