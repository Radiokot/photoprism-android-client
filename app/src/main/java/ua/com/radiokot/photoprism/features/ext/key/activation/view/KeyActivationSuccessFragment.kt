package ua.com.radiokot.photoprism.features.ext.key.activation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import ua.com.radiokot.photoprism.databinding.FragmentKeyInputSuccessBinding
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.ext.key.activation.view.model.GalleryExtensionListItem
import ua.com.radiokot.photoprism.features.ext.key.activation.view.model.KeyActivationViewModel

class KeyActivationSuccessFragment : Fragment() {
    private lateinit var view: FragmentKeyInputSuccessBinding
    private val viewModel: KeyActivationViewModel
        get() = (requireActivity() as KeyActivationActivity).viewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = FragmentKeyInputSuccessBinding.inflate(inflater)
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initList()
        initButtons()
    }

    private fun initList() {
        val successState =
            checkNotNull(viewModel.currentState as? KeyActivationViewModel.State.Success) {
                "This screen can only be shown in the successful state"
            }

        // TODO show expiration if set, above or below the list
        view.addedExtensionsRecyclerView.adapter = FastAdapter.with(
            ItemAdapter<GalleryExtensionListItem>()
                .setNewList(
                    successState.addedExtensions
                        .map(::GalleryExtensionListItem)
                )
        )
    }

    private fun initButtons() {
        with(view.doneButton) {
            setThrottleOnClickListener {
                viewModel.onSuccessDoneClicked()
            }
        }
    }
}
