package ua.com.radiokot.photoprism.features.gallery.view

import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadProgressViewModel

class DownloadProgressView(
    private val viewModel: DownloadProgressViewModel,
    private val fragmentManager: FragmentManager,
    private val errorSnackbarView: View,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {
    private val log = kLogger("DownloadProgressView")

    fun init() {
        subscribeToState()
        subscribeToEvents()

        fragmentManager.setFragmentResultListener(
            DownloadProgressDialogFragment.CANCELLATION_REQUEST_KEY,
            this
        ) { _, _ ->
            viewModel.onUserCancelledDownload()
        }
    }

    private fun subscribeToState() = viewModel.downloadProgressState.subscribe(this) { state ->
        when (state) {
            DownloadProgressViewModel.State.Idle ->
                dismissDownloadProgress()

            is DownloadProgressViewModel.State.Running ->
                showDownloadProgress(
                    percent = state.percent,
                    currentDownloadNumber = state.currentDownloadNumber,
                    downloadsCount = state.downloadsCount,
                )
        }
    }

    private fun subscribeToEvents(
    ) = viewModel.downloadProgressEvents.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            DownloadProgressViewModel.Event.DownloadFailed ->
                showDownloadError()
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

    private fun showDownloadProgress(
        percent: Int,
        currentDownloadNumber: Int = 1,
        downloadsCount: Int = 1,
    ) {
        val fragment =
            (fragmentManager.findFragmentByTag(DOWNLOAD_PROGRESS_DIALOG_TAG) as? DownloadProgressDialogFragment)
                ?: DownloadProgressDialogFragment()

        if (!fragment.isAdded || !fragment.showsDialog) {
            fragment.showNow(fragmentManager, DOWNLOAD_PROGRESS_DIALOG_TAG)
        }

        fragment.setProgress(percent, currentDownloadNumber, downloadsCount)
    }

    private fun dismissDownloadProgress() {
        (fragmentManager.findFragmentByTag(DOWNLOAD_PROGRESS_DIALOG_TAG) as? DialogFragment)
            ?.dismiss()
    }

    private fun showDownloadError() {
        Snackbar.make(
            errorSnackbarView,
            R.string.failed_to_download_file,
            Snackbar.LENGTH_SHORT
        )
            .show()
    }

    private companion object {
        private const val DOWNLOAD_PROGRESS_DIALOG_TAG = "download-progress"
    }
}
