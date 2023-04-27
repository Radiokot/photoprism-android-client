package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.isSameYearAs
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.GalleryMonthsSequence
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GalleryFastScrollViewModel(
    private val bubbleMonthYearDateFormat: DateFormat,
    private val bubbleMonthDateFormat: DateFormat,
) : ViewModel() {
    private val log = kLogger("GalleryFastScrollVM")
    private var currentMediaRepository: SimpleGalleryMediaRepository? = null
    private val monthsDraggingSubject: PublishSubject<GalleryMonthScrollBubble> =
        PublishSubject.create()
    private val monthsDragEndedSubject: PublishSubject<GalleryMonthScrollBubble> =
        PublishSubject.create()

    val bubbles = MutableLiveData<List<GalleryMonthScrollBubble>>(emptyList())
    val state: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Idle)

    init {
        subscribeToDragging()
    }

    private fun subscribeToDragging() {
        // Combine months dragging and drag ended to make the scroll bar responsive.
        Observable.merge(
            monthsDragEndedSubject,
            monthsDraggingSubject
                .debounce(
                    DRAGGING_DEBOUNCE_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS,
                    Schedulers.newThread()
                )
        )
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { monthBubble ->
                log.debug {
                    "subscribeToDragging(): setting_the_month:" +
                            "\nbubble=$monthBubble"
                }

                state.onNext(
                    State.AtMonth(
                        monthBubble = monthBubble,
                        isTopMonth = bubbles.value?.firstOrNull() == monthBubble,
                    )
                )
            }
            .addToCloseables(this)
    }

    fun setMediaRepository(mediaRepository: SimpleGalleryMediaRepository) {
        if (mediaRepository != currentMediaRepository) {
            log.debug {
                "setMediaRepository(): set_new_repo:" +
                        "\nmediaRepository=$mediaRepository"
            }
            currentMediaRepository = mediaRepository
            updateBubbles(mediaRepository)
        } else {
            log.debug {
                "setMediaRepository(): already_set"
            }
        }
    }

    private var bubblesUpdateDisposable: Disposable? = null
    private fun updateBubbles(mediaRepository: SimpleGalleryMediaRepository) {
        bubblesUpdateDisposable?.dispose()
        bubblesUpdateDisposable = mediaRepository.getNewestAndOldestDates()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                bubbles.value = emptyList()
                state.onNext(State.Loading)
            }
            .doOnTerminate { state.onNext(State.Idle) }
            .subscribeBy(
                onError = { error ->
                    log.error(error) { "updateBubbles(): error_occurred" }
                },
                onComplete = {
                    log.debug { "updateBubbles(): no_dates_available" }
                },
                onSuccess = { (newestDate, oldestDate) ->
                    log.debug {
                        "updateBubbles(): got_dates:" +
                                "\nnewest=$newestDate," +
                                "\noldest=$oldestDate"
                    }

                    createBubbles(newestDate, oldestDate)
                }
            )
    }

    private fun createBubbles(newestDate: Date, oldestDate: Date) {
        val today = Date()

        bubbles.value =
            GalleryMonthsSequence(
                startDate = oldestDate,
                endDate = newestDate
            )
                .toList()
                .sortedDescending()
                .map { month ->
                    GalleryMonthScrollBubble(
                        name =
                        if (month.firstDay.isSameYearAs(today))
                            bubbleMonthDateFormat.format(month.firstDay)
                        else
                            bubbleMonthYearDateFormat.format(month.firstDay),
                        source = month
                    )
                }
                .also {
                    log.debug {
                        "createBubbles(): bubbles_created:" +
                                "\ncount=${it.size}"
                    }
                }
    }

    fun reset() {
        log.debug { "reset(): resetting_to_idle" }

        state.onNext(State.Idle)
    }

    fun onDragging(monthBubble: GalleryMonthScrollBubble) {
        monthsDraggingSubject.onNext(monthBubble)
    }

    fun onDragEnded(monthBubble: GalleryMonthScrollBubble) {
        monthsDragEndedSubject.onNext(monthBubble)
    }

    sealed interface State {
        object Loading : State
        object Idle : State
        class AtMonth(
            val monthBubble: GalleryMonthScrollBubble,
            val isTopMonth: Boolean
        ) : State
    }

    private companion object {
        private const val DRAGGING_DEBOUNCE_TIMEOUT_MS = 200L
    }
}