package ua.com.radiokot.photoprism.features.gallery.view.model

import io.reactivex.rxjava3.core.Observable

interface DownloadProgressViewModel {
    val downloadState: Observable<State>
    val downloadEvents: Observable<Event>

    fun onDownloadProgressDialogCancelled()

    sealed interface State {
        object Idle : State
        class Running(
            val percent: Double,
            val currentDownloadNumber: Int = 1,
            val downloadsCount: Int = 1,
        ) : State
    }

    sealed interface Event {
        object DownloadFailed : Event
    }
}