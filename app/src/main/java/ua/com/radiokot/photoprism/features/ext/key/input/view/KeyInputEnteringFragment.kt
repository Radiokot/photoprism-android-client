package ua.com.radiokot.photoprism.features.ext.key.input.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import ua.com.radiokot.photoprism.databinding.FragmentKeyInputEnteringBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.ext.key.input.view.model.KeyInputViewModel

class KeyInputEnteringFragment : Fragment() {
    private lateinit var view: FragmentKeyInputEnteringBinding
    private val viewModel: KeyInputViewModel
        get() = (requireActivity() as KeyInputActivity).viewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = FragmentKeyInputEnteringBinding.inflate(inflater)
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initFields()
        initButtons()
    }

    private fun initFields() {
        with(view.keyTextInput) {
            with(editText!!) {
                setLines(4)
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

            viewModel.keyError.observe(viewLifecycleOwner) { keyError ->
                isErrorEnabled = keyError != null
                error = when (keyError) {
                    null ->
                        null

                    else ->
                        keyError.toString()
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
    }
}
