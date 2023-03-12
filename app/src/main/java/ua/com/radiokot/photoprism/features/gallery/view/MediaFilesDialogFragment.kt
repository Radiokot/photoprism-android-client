package ua.com.radiokot.photoprism.features.gallery.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.DialogMediaFilesBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem


class MediaFilesDialogFragment : DialogFragment(R.layout.dialog_media_files) {
    private val log = kLogger("MMediaFilesDialog")

    private lateinit var viewBinding: DialogMediaFilesBinding

    private val files: List<GalleryMedia.File> by lazy {
        @Suppress("DEPRECATION")
        requireArguments().getParcelableArrayList(FILES_KEY)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = onCreateView(layoutInflater, null, savedInstanceState)
            .checkNotNull()

        viewBinding = DialogMediaFilesBinding.bind(view)

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setView(view)

        log.debug {
            "onCreateDialog(): material_builder_initialized"
        }

        return builder.create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log.debug {
            "onViewCreated(): created:" +
                    "\nsavedInstanceState=$savedInstanceState," +
                    "\nfiles=$files"
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
                            "\nsource=${item.source}"

                    if (item.source != null) {
                        onFileSelected(item.source)
                    }
                }

                false
            }
        }

        with(viewBinding.filesList) {
            adapter = fastAdapter
            layoutManager = LinearLayoutManager(context)
        }

        filesAdapter.setNewList(files.map {
            MediaFileListItem(
                source = it,
                context = requireContext()
            )
        })
    }

    private fun onFileSelected(file: GalleryMedia.File) {
        log.debug {
            "onFileSelected(): set_result:" +
                    "\nfile=$file"
        }

        setFragmentResult(REQUEST_KEY, Bundle().apply {
            putParcelable(REQUEST_KEY, file)
        })

        dismiss()
    }

    companion object {
        private const val FILES_KEY = "files"
        const val REQUEST_KEY = "file-selection"

        fun getBundle(files: List<GalleryMedia.File>): Bundle = Bundle().apply {
            putParcelableArrayList(FILES_KEY, ArrayList(files))
        }

        @Suppress("DEPRECATION")
        fun getResult(bundle: Bundle): GalleryMedia.File =
            bundle.getParcelable(REQUEST_KEY)!!
    }
}