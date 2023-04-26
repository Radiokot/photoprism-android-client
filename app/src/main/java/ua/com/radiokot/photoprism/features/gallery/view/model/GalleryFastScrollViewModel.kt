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

    val bubbles = MutableLiveData<List<GalleryMonthScrollBubble>>(emptyList())
    val state: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Idle)

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

    private val monthsDraggingSubject: PublishSubject<GalleryMonthScrollBubble> =
        PublishSubject.create()
    private val monthsDraggedSubject: PublishSubject<GalleryMonthScrollBubble> =
        PublishSubject.create()

    init {
        Observable.merge(
            monthsDraggedSubject,
            monthsDraggingSubject
                .debounce(300, TimeUnit.MILLISECONDS, Schedulers.newThread())
        )
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { monthBubble ->
                state.onNext(
                    State.AtMonth(
                        monthBubble = monthBubble,
                        isTopMonth = bubbles.value?.firstOrNull() == monthBubble,
                    )
                )
            }
            .addToCloseables(this)
    }

    fun onDraggingOnMonth(monthBubble: GalleryMonthScrollBubble) {
        monthsDraggingSubject.onNext(monthBubble)
//        log.debug {
//            "onScrolledToMonth(): scrolled_to_month:" +
//                    "\nmonthBubble=$monthBubble"
//        }
//
//        state.onNext(
//            State.AtMonth(
//                monthBubble = monthBubble,
//                isTopMonth = bubbles.value?.firstOrNull() == monthBubble,
//            )
//        )
    }

    fun onDraggedToMonth(monthBubble: GalleryMonthScrollBubble) {
        monthsDraggedSubject.onNext(monthBubble)
    }

    sealed interface State {
        object Loading : State
        object Idle : State
        class AtMonth(
            val monthBubble: GalleryMonthScrollBubble,
            val isTopMonth: Boolean
        ) : State
    }
}