package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.filterIsInstance
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMonth
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.GalleryMonthsSequence
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.concurrent.TimeUnit

class GalleryFastScrollViewModel: ViewModel() {
    private val log = kLogger("GalleryFastScrollVM")
    private var currentMediaRepository: SimpleGalleryMediaRepository? = null

    // A subject to post the current bubble when dragging.
    private val monthsDraggingSubject = PublishSubject.create<GalleryMonthScrollBubble>()

    // A subject to post the current bubble when the drag is ended.
    private val monthsDragEndedSubject = PublishSubject.create<GalleryMonthScrollBubble>()

    // A subject to post the reset signal.
    private val monthsDragResetSubject = PublishSubject.create<Unit>()

    val bubbles = MutableLiveData<List<GalleryMonthScrollBubble>>(emptyList())
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOnMain()

    init {
        subscribeToDragging()
    }

    private fun subscribeToDragging() {
        // Combine months dragging and drag ended to make the scroll bar responsive.
        Observable.merge(
            monthsDragResetSubject,
            monthsDragEndedSubject,
            monthsDraggingSubject
                .debounce(
                    DRAGGING_DEBOUNCE_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS,
                    Schedulers.newThread()
                )
        )
            .filterIsInstance<GalleryMonthScrollBubble>()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { monthBubble ->
                val month = monthBubble.source
                    ?: return@subscribeBy

                log.debug {
                    "subscribeToDragging(): posting_drag_event:" +
                            "\nmonth=$month"
                }

                eventsSubject.onNext(
                    Event.DraggingAtMonth(
                        month = month,
                        isTopMonth = bubbles.value?.firstOrNull() == monthBubble
                    )
                )
            }
            .autoDispose(this)
    }

    fun setMediaRepository(mediaRepository: SimpleGalleryMediaRepository) {
        if (mediaRepository != currentMediaRepository) {
            log.debug {
                "setMediaRepository(): set_new_repo:" +
                        "\nmediaRepository=$mediaRepository"
            }
            currentMediaRepository = mediaRepository
            updateBubbles()
        } else {
            log.debug {
                "setMediaRepository(): already_set"
            }
        }
    }

    private var bubblesUpdateDisposable: Disposable? = null
    fun updateBubbles() {
        val mediaRepository = currentMediaRepository
            ?: return

        bubblesUpdateDisposable?.dispose()
        bubblesUpdateDisposable = mediaRepository.getNewestAndOldestLocalDates()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                bubbles.value = emptyList()
            }
            .doOnSuccess { (newestDate, oldestDate) ->
                log.debug {
                    "updateBubbles(): got_dates:" +
                            "\nnewest=$newestDate," +
                            "\noldest=$oldestDate"
                }

                // Create bubbles here to gracefully handle possible errors.
                // Yet, the cause of weird dates needs to be investigated.
                createBubbles(newestDate, oldestDate)
            }
            .subscribeBy(
                onError = { error ->
                    log.error(error) { "updateBubbles(): error_occurred" }
                },
                onComplete = {
                    log.debug { "updateBubbles(): no_dates_available" }
                },
            )
    }

    private fun createBubbles(newestDate: LocalDate, oldestDate: LocalDate) {
        val currentLocalDate = LocalDate()
        bubbles.value =
            GalleryMonthsSequence(
                startLocalDate = oldestDate,
                endLocalDate = newestDate
            )
                .toList()
                .sortedDescending()
                .map { month ->
                    GalleryMonthScrollBubble(
                        localDate = month.firstDay,
                        withYear = !month.firstDay.isSameYearAs(currentLocalDate),
                        source = month,
                    )
                }
                .also {
                    log.debug {
                        "createBubbles(): bubbles_created:" +
                                "\ncount=${it.size}"
                    }
                }
    }

    /**
     * Reset of the month drag and the scroll (goes back to the top).
     *
     * @see Event.Reset
     */
    fun reset(isInitiatedByUser: Boolean) {
        log.debug {
            "reset(): resetting_drag_and_scroll:" +
                    "\nisInitiatedByUser=$isInitiatedByUser"
        }

        monthsDragResetSubject.onNext(Unit)
        eventsSubject.onNext(Event.Reset(isInitiatedByUser))
    }

    fun onDragging(monthBubble: GalleryMonthScrollBubble) {
        monthsDraggingSubject.onNext(monthBubble)
    }

    fun onDragEnded(monthBubble: GalleryMonthScrollBubble) {
        monthsDragEndedSubject.onNext(monthBubble)
    }

    sealed interface Event {
        data class DraggingAtMonth(
            val month: GalleryMonth,
            val isTopMonth: Boolean,
        ) : Event

        /**
         * Reset of the month drag and the scroll (goes back to the top).
         */
        data class Reset(val isInitiatedByUser: Boolean) : Event
    }

    private companion object {
        private const val DRAGGING_DEBOUNCE_TIMEOUT_MS = 200L
    }
}
