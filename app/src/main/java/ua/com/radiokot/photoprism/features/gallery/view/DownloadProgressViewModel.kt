package ua.com.radiokot.photoprism.features.gallery.view

import io.reactivex.rxjava3.core.Observable

interface DownloadProgressViewModel {
    val downloadState: Observable<State>
    val downloadEvents: Observable<Event>

    fun onDownloadProgressDialogCancelled()

    sealed interface State {
        object Idle : State
        class Running(val percent: Double) : State
    }

    sealed interface Event {
        object DownloadFailed : Event
    }
}