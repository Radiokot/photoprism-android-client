package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.isSameYearAs
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMonth
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryMonthsRepository
import java.text.DateFormat
import java.util.*

class GalleryFastScrollViewModel(
    private val galleryMonthsRepository: GalleryMonthsRepository,
    private val bubbleMonthYearDateFormat: DateFormat,
    private val bubbleMonthDateFormat: DateFormat,
) : ViewModel() {
    private val log = kLogger("GalleryFastScrollVM")

    val bubbles = MutableLiveData<List<GalleryMonthScrollBubble>>(emptyList())
    private val stateSubject = BehaviorSubject.createDefault<State>(State.Idle)
    val state: Observable<State> = stateSubject

    init {

    }

    private var repositoryUpdateDisposable: Disposable? = null
    fun setSearchQuery(query: String?) {
        log.debug {
            "setSearchQuery(): loading_months:" +
                    "\nquery=$query"
        }

        repositoryUpdateDisposable?.dispose()
        repositoryUpdateDisposable = galleryMonthsRepository
            .update(galleryMediaQuery = query)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                bubbles.value = emptyList()
                stateSubject.onNext(State.Loading)
            }
            .doOnTerminate { stateSubject.onNext(State.Idle) }
            .subscribeBy(
                onError = { error ->
                    log.error(error) { "setSearchQuery(): error_occurred" }
                },
                onSuccess = { months ->
                    log.debug {
                        "setSearchQuery(): loaded_months:" +
                                "\nsize=${months.size}"
                    }

                    createBubbles(months)
                }
            )
    }

    private fun createBubbles(months: List<GalleryMonth>) {
        val today = Date()
        bubbles.value = months
            .map { month ->
                val monthEnd = month.end

                GalleryMonthScrollBubble(
                    name =
                    if (monthEnd.isSameYearAs(today))
                        bubbleMonthDateFormat.format(monthEnd)
                    else
                        bubbleMonthYearDateFormat.format(monthEnd),
                    source = month
                )
            }
    }

    fun reset() {

    }

    fun onScrolledToMonth(monthBubble: GalleryMonthScrollBubble) {
        log.debug {
            "onScrolledToMonth(): scrolled_to_month:" +
                    "\nmonthBubble=$monthBubble"
        }

        stateSubject.onNext(State.AtMonth(monthBubble = monthBubble))
    }

    sealed interface State {
        object Loading : State
        object Idle : State
        class AtMonth(val monthBubble: GalleryMonthScrollBubble) : State
    }
}