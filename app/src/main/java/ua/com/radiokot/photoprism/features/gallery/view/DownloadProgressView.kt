package ua.com.radiokot.photoprism.features.gallery.view

import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
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
    }

    private fun subscribeToState() {
        viewModel.downloadState.subscribe { state ->
            log.debug {
                "subscribeToState(): received_new_state:" +
                        "\nstate=$state"
            }

            when (state) {
                DownloadProgressViewModel.State.Idle ->
                    dismissDownloadProgress()
                is DownloadProgressViewModel.State.Running ->
                    showDownloadProgress(
                        percent = state.percent,
                    )
            }

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.disposeOnDestroy(this)
    }

    private fun subscribeToEvents() {
        viewModel.downloadEvents.subscribe { event ->
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
        }.disposeOnDestroy(this)
    }

    private fun showDownloadProgress(percent: Double) {
        val fragment =
            (fragmentManager.findFragmentByTag(DOWNLOAD_PROGRESS_DIALOG_TAG) as? DownloadProgressDialogFragment)
                ?: DownloadProgressDialogFragment().apply {
                    cancellationEvent.observe(this@DownloadProgressView) {
                        viewModel.onDownloadProgressDialogCancelled()
                    }
                }

        if (!fragment.isAdded || !fragment.showsDialog) {
            fragment.showNow(fragmentManager, DOWNLOAD_PROGRESS_DIALOG_TAG)
        }

        fragment.setProgress(percent)
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