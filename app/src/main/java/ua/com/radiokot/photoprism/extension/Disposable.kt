package ua.com.radiokot.photoprism.extension

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.Disposable

fun <T : Disposable> T.addToCloseables(viewModel: ViewModel) = apply {
    viewModel.addCloseable { dispose() }
}