package ua.com.radiokot.photoprism.features.gallery.view

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.MutableLiveData
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseMaterialDialogFragment
import ua.com.radiokot.photoprism.databinding.DialogDownloadProgressBinding
import kotlin.math.roundToInt

class DownloadProgressDialogFragment :
    BaseMaterialDialogFragment(R.layout.dialog_download_progress) {

    private lateinit var viewBinding: DialogDownloadProgressBinding
    private val progressPercent: MutableLiveData<Double> = MutableLiveData()

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
    }

    /**
     * @param percent from 0 to 100 or -1 for indeterminate.
     */
    fun setProgress(percent: Double) = apply {
        progressPercent.value = percent
    }

    override fun onCancel(dialog: DialogInterface) {
        cancellationEvent.value = Unit
    }
}