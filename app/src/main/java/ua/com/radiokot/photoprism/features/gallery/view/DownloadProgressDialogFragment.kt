package ua.com.radiokot.photoprism.features.gallery.view

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogDownloadProgressBinding
import kotlin.math.roundToInt

class DownloadProgressDialogFragment :
    BaseMaterialDialogFragment(R.layout.dialog_download_progress) {

    private lateinit var viewBinding: DialogDownloadProgressBinding
    private val progressPercent: MutableLiveData<Double> = MutableLiveData()
    private val currentDownloadNumberOf: MutableLiveData<Pair<Int, Int>> = MutableLiveData()

    val cancellationEvent: MutableLiveData<Unit> = MutableLiveData()

    override fun onDialogViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        viewBinding = DialogDownloadProgressBinding.bind(dialogView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initButtons()
        initProgress()
    }

    private fun initButtons() {
        viewBinding.closeButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    private fun initProgress() {
        with(viewBinding.progressIndicator) {
            max = 10000

            progressPercent.observe(viewLifecycleOwner) { percent ->
                if (percent < 0) {
                    isIndeterminate = true
                } else {
                    isIndeterminate = false
                    progress = ((max / 100) * percent).roundToInt()
                }
            }
        }

        with(viewBinding.currentDownloadNumberOfTextView) {
            currentDownloadNumberOf.observe(viewLifecycleOwner) { (number, of) ->
                isVisible = of > 1

                if (isVisible) {
                    text = getString(
                        R.string.template_current_of,
                        number,
                        of
                    )
                }
            }
        }
    }

    /**
     * @param percent from 0 to 100 or -1 for indeterminate.
     * @param currentDownloadNumber number of the current download.
     * @param downloadsCount count of downloads to perform in series.
     */
    fun setProgress(
        percent: Double,
        currentDownloadNumber: Int = 1,
        downloadsCount: Int = 1,
    ) = apply {
        progressPercent.value = percent
        currentDownloadNumberOf.value = currentDownloadNumber to downloadsCount
    }

    override fun onCancel(dialog: DialogInterface) {
        cancellationEvent.value = Unit
    }
}
