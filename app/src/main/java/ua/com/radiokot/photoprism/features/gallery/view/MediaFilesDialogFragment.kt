package ua.com.radiokot.photoprism.features.gallery.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogMediaFilesBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem


class MediaFilesDialogFragment : BaseMaterialDialogFragment(R.layout.dialog_media_files) {
    private val log = kLogger("MMediaFilesDialog")

    private lateinit var viewBinding: DialogMediaFilesBinding

    private val fileItems: List<MediaFileListItem> by lazy {
        @Suppress("DEPRECATION")
        requireArguments().getParcelableArrayList(FILES_KEY)!!
    }

    override fun onDialogViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        viewBinding = DialogMediaFilesBinding.bind(dialogView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log.debug {
            "onViewCreated(): created:" +
                    "\nsavedInstanceState=$savedInstanceState," +
                    "\nfileItems=$fileItems"
        }

        initList()
    }

    private fun initList() {
        val filesAdapter = ItemAdapter<MediaFileListItem>()
        val fastAdapter = FastAdapter.with(filesAdapter).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item, _ ->
                log.debug {
                    "file_item_clicked:" +
                            "\nitem=$item"
                }

                onFileSelected(item)

                false
            }
        }

        with(viewBinding.filesList) {
            adapter = fastAdapter
            layoutManager = LinearLayoutManager(context)
        }

        filesAdapter.setNewList(fileItems)
    }

    private fun onFileSelected(fileItem: MediaFileListItem) {
        log.debug {
            "onFileSelected(): set_result:" +
                    "\nfileItem=$fileItem"
        }

        setFragmentResult(REQUEST_KEY, Bundle().apply {
            putParcelable(REQUEST_KEY, fileItem)
        })

        dismiss()
    }

    companion object {
        private const val FILES_KEY = "files"
        const val REQUEST_KEY = "file-selection"

        fun getBundle(fileItems: List<MediaFileListItem>): Bundle = Bundle().apply {
            putParcelableArrayList(FILES_KEY, ArrayList(fileItems))
        }

        @Suppress("DEPRECATION")
        fun getResult(bundle: Bundle): MediaFileListItem =
            bundle.getParcelable(REQUEST_KEY)!!
    }
}