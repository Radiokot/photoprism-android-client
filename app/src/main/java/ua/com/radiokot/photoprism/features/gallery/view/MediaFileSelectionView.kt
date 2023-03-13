package ua.com.radiokot.photoprism.features.gallery.view

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem

class MediaFileSelectionView(
    private val fragmentManager: FragmentManager,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {
    private val log = kLogger("MediaFileSelectionView")

    fun init(
        onFileSelected: (item: MediaFileListItem) -> Unit,
    ) {
        fragmentManager.setFragmentResultListener(
            MediaFilesDialogFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            val selectedFile = MediaFilesDialogFragment.getResult(bundle)

            log.debug {
                "onFragmentResult(): got_selected_media_file:" +
                        "\nfile=$selectedFile"
            }

            onFileSelected(selectedFile)
        }
    }

    fun openMediaFileSelectionDialog(fileItems: List<MediaFileListItem>) {
        val fragment =
            (fragmentManager.findFragmentByTag(FILES_DIALOG_TAG) as? MediaFilesDialogFragment)
                ?: MediaFilesDialogFragment().apply {
                    arguments = MediaFilesDialogFragment.getBundle(fileItems)
                }

        if (!fragment.isAdded || !fragment.showsDialog) {
            fragment.showNow(fragmentManager, FILES_DIALOG_TAG)
        }
    }

    private companion object {
        private const val FILES_DIALOG_TAG = "media-files"
    }
}