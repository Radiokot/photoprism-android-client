package ua.com.radiokot.photoprism.features.gallery.view.model

import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject

interface DownloadProgressViewModel {
    val downloadProgressState: BehaviorSubject<State>
    val downloadProgressEvents: PublishSubject<Event>

    fun onUserCancelledDownload()

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
