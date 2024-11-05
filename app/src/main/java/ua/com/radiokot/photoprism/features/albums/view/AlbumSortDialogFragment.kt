package ua.com.radiokot.photoprism.features.albums.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.setFragmentResult
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogAlbumSortBinding
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSortResources

class AlbumSortDialogFragment :
    BaseMaterialDialogFragment(R.layout.dialog_album_sort) {

    private lateinit var viewBinding: DialogAlbumSortBinding
    private val initialSort: AlbumSort by lazy {
        getResult(requireArguments())
    }

    override fun onDialogViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        viewBinding = DialogAlbumSortBinding.bind(dialogView)
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
            AlbumSort.Order.values().map { order ->
                getString(AlbumSortResources.getName(order))
            }
        )
        setItemChecked(
            AlbumSort.Order.values().indexOf(initialSort.order),
            true
        )
    }

    private fun initOptions() {
        viewBinding.favoritesFirstCheckBox.isChecked = initialSort.areFavoritesFirst
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
                AlbumSort(
                    order = AlbumSort.Order.values()[viewBinding.orderListView.checkedItemPosition],
                    areFavoritesFirst = viewBinding.favoritesFirstCheckBox.isChecked,
                )
            )
        )
        dismiss()
    }

    companion object {
        private const val SORT_EXTRA = "sort"
        const val TAG = "album-sort"
        const val REQUEST_KEY = TAG

        fun getBundle(
            sort: AlbumSort,
        ) = Bundle().apply {
            putParcelable(SORT_EXTRA, sort)
        }

        @Suppress("DEPRECATION")
        fun getResult(bundle: Bundle): AlbumSort =
            bundle.getParcelable(SORT_EXTRA)!!
    }
}
