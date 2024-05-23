package ua.com.radiokot.photoprism.features.gallery.view.model

import io.reactivex.rxjava3.core.Observable

interface DownloadProgressViewModel {
    val downloadState: Observable<State>
    val downloadEvents: Observable<Event>

    fun onDownloadProgressDialogCancelled()

    sealed interface State {
        object Idle : State
        class Running(
            /**
             * From 0 to 100, negative for indeterminate.
             */
            val percent: Int = -1,
            val currentDownloadNumber: Int = 1,
            val downloadsCount: Int = 1,
        ) : State
    }

    sealed interface Event {
        object DownloadFailed : Event
    }
}
