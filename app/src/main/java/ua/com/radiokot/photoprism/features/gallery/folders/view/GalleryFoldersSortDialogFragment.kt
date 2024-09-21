package ua.com.radiokot.photoprism.features.gallery.folders.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.setFragmentResult
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogGalleryFoldersSortBinding
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.gallery.folders.view.model.GalleryFolderOrder
import ua.com.radiokot.photoprism.features.gallery.folders.view.model.GalleryFolderOrderResources

class GalleryFoldersSortDialogFragment :
    BaseMaterialDialogFragment(R.layout.dialog_gallery_folders_sort) {

    private lateinit var viewBinding: DialogGalleryFoldersSortBinding

    override fun onDialogViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        viewBinding = DialogGalleryFoldersSortBinding.bind(dialogView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initList()
        initOptions()
        initButtons()
    }

    private fun initList() = with(viewBinding.orderListView) {
        adapter = ArrayAdapter(
            requireContext(),
            R.layout.select_dialog_singlechoice_material,
            android.R.id.text1,
            GalleryFolderOrder.values().map { order ->
                getString(GalleryFolderOrderResources.getName(order))
            }
        )
        setItemChecked(
            GalleryFolderOrder.values().indexOf(
                getSelectedOrder(requireArguments())
            ),
            true
        )
    }

    private fun initOptions() {
        viewBinding.favoritesFirstCheckBox.isChecked = areFavoritesFirst(requireArguments())
    }

    private fun initButtons() {
        viewBinding.okButton.setThrottleOnClickListener {
            dismissWithResult()
        }

        viewBinding.closeButton.setThrottleOnClickListener {
            dismiss()
        }
    }

    private fun dismissWithResult() {
        setFragmentResult(
            REQUEST_KEY,
            getBundle(
                selectedOrder = GalleryFolderOrder.values()[viewBinding.orderListView.checkedItemPosition],
                areFavoritesFirst = viewBinding.favoritesFirstCheckBox.isChecked,
            )
        )
        dismiss()
    }

    companion object {
        private const val SELECTED_ORDER_EXTRA = "selected-order"
        private const val ARE_FAVORITES_FIRST_EXTRA = "are-favorites-first"
        const val REQUEST_KEY = "folders-sort"
        const val TAG = "folders-sort"

        fun getBundle(
            selectedOrder: GalleryFolderOrder,
            areFavoritesFirst: Boolean,
        ) = Bundle().apply {
            putSerializable(SELECTED_ORDER_EXTRA, selectedOrder)
            putBoolean(ARE_FAVORITES_FIRST_EXTRA, areFavoritesFirst)
        }

        fun getSelectedOrder(bundle: Bundle): GalleryFolderOrder =
            bundle.getSerializable(SELECTED_ORDER_EXTRA, GalleryFolderOrder::class.java)!!

        fun areFavoritesFirst(bundle: Bundle): Boolean =
            bundle.getBoolean(ARE_FAVORITES_FIRST_EXTRA, false)
    }
}
