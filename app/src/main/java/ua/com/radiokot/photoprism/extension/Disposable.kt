package ua.com.radiokot.photoprism.extension

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.Disposable

fun <T : Disposable> T.addToCloseables(viewModel: ViewModel) = apply {
    viewModel.addCloseable {
        if (!isDisposed) {
            dispose()
        }
    }
}

private class LifecycleDisposable(obj: Disposable) :
    DefaultLifecycleObserver, Disposable by obj {
    override fun onDestroy(owner: LifecycleOwner) {
        if (!isDisposed) {
            dispose()
        }
    }
}

fun <T : Disposable> T.disposeOnDestroy(owner: LifecycleOwner) = apply {
    owner.lifecycle.addObserver(LifecycleDisposable(this))
}