package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import java.util.concurrent.TimeUnit

class GallerySearchViewModel : ViewModel() {
    private val log = kLogger("GallerySearchViewModel")

    val availableMediaTypes = MutableLiveData(
        setOf(
            GalleryMedia.TypeName.IMAGE,
            GalleryMedia.TypeName.VIDEO,
            GalleryMedia.TypeName.ANIMATED,
            GalleryMedia.TypeName.LIVE,
            GalleryMedia.TypeName.RAW,
            GalleryMedia.TypeName.VECTOR,
        )
    )
    val selectedMediaTypes = MutableLiveData<Set<GalleryMedia.TypeName>>()
    val userQuery = MutableLiveData<String>()
    val isApplyButtonEnabled = MutableLiveData(false)
    private val stateSubject = BehaviorSubject.createDefault<State>(State.NoSearch)
    val state: Observable<State> = stateSubject
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject
    val bookmarks = MutableLiveData<List<SearchBookmarkItem>>()
    val selectedBookmark = MutableLiveData<SearchBookmarkItem?>(null)

    init {
        selectedMediaTypes.observeForever {
            isApplyButtonEnabled.value = canApplyConfiguredSearch
        }
        userQuery.observeForever {
            isApplyButtonEnabled.value = canApplyConfiguredSearch
        }

        // TODO: Remove
        Observable.timer(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { eventsSubject.onNext(Event.OpenBookmarkDialog(bookmark = SearchBookmark("Oleg"))) }
            .addToCloseables(this)
        bookmarks.value = listOf(
            SearchBookmarkItem(name = "My Screenshots", source = null),
            SearchBookmarkItem(name = "Yasya Camera", source = null),
            SearchBookmarkItem(name = "My camera", source = null),
            SearchBookmarkItem(name = "TikToks", source = null),
        ).shuffled()
        selectedBookmark.value = SearchBookmarkItem(name = "My camera", source = null)
    }

    private val canApplyConfiguredSearch: Boolean
        get() = !selectedMediaTypes.value.isNullOrEmpty()
                || !userQuery.value.isNullOrBlank()

    fun onAvailableMediaTypeClicked(typeName: GalleryMedia.TypeName) {
        val currentlySelected = selectedMediaTypes.value ?: emptySet()
        if (currentlySelected.contains(typeName)) {
            log.debug {
                "onAvailableMediaTypeClicked(): unselect:" +
                        "\ntypeName=$typeName"
            }
            selectedMediaTypes.value = currentlySelected - typeName
        } else {
            log.debug {
                "onAvailableMediaTypeClicked(): select:" +
                        "\ntypeName=$typeName"
            }
            selectedMediaTypes.value = currentlySelected + typeName
        }
    }

    fun onConfigurationViewOpening() {
        log.debug {
            "onConfigurationViewOpening(): configuration_view_is_opening"
        }

        when (val state = stateSubject.value!!) {
            is State.AppliedSearch -> {
                selectedMediaTypes.value = state.search.mediaTypes
                userQuery.value = state.search.userQuery

                stateSubject.onNext(
                    State.ConfiguringSearch(
                        alreadyAppliedSearch = state.search,
                    )
                )
            }

            State.NoSearch -> {
                selectedMediaTypes.value = emptySet()
                userQuery.value = ""

                stateSubject.onNext(
                    State.ConfiguringSearch(
                        alreadyAppliedSearch = null
                    )
                )
            }

            is State.ConfiguringSearch -> {
                selectedMediaTypes.value = state.alreadyAppliedSearch?.mediaTypes ?: emptySet()
                userQuery.value = state.alreadyAppliedSearch?.userQuery

                stateSubject.onNext(
                    State.ConfiguringSearch(
                        alreadyAppliedSearch = state.alreadyAppliedSearch
                    )
                )
            }
        }

    }

    fun onConfigurationViewClosing() {
        log.debug {
            "onConfigurationViewClosing(): configuration_view_is_closing"
        }

        when (val state = stateSubject.value!!) {
            is State.AppliedSearch,
            is State.NoSearch -> {
                // Expected.
            }
            is State.ConfiguringSearch -> {
                if (state.alreadyAppliedSearch != null) {
                    stateSubject.onNext(State.AppliedSearch(state.alreadyAppliedSearch))
                } else {
                    stateSubject.onNext(State.NoSearch)
                }
            }
        }
    }

    fun onSearchClicked() {
        log.debug {
            "onSearchClicked(): search_clicked:" +
                    "\ncanApplyConfiguredSearch=$canApplyConfiguredSearch"
        }

        if (canApplyConfiguredSearch) {
            applyConfiguredSearch()
        }
    }

    private fun applyConfiguredSearch() {
        check(stateSubject.value is State.ConfiguringSearch) {
            "The search can only be applied while configuring"
        }

        val search = AppliedGallerySearch(
            mediaTypes = selectedMediaTypes.value ?: emptySet(),
            userQuery = userQuery.value!!.trim(),
            bookmark = null
        )

        log.debug {
            "applySearch(): applying_search:" +
                    "\nsearch=$search"
        }

        stateSubject.onNext(State.AppliedSearch(search))
    }

    fun onResetClicked() {
        log.debug {
            "onResetClicked(): reset_clicked"
        }

        stateSubject.onNext(State.NoSearch)
    }

    fun onAddBookmarkClicked() {
        check(stateSubject.value is State.AppliedSearch) {
            "Add bookmark button is only clickable in the applied search state"
        }

        log.debug {
            "onAddBookmarkClicked(): add_bookmark_clicked"
        }
    }

    fun onEditBookmarkClicked() {
        check(stateSubject.value is State.AppliedSearch) {
            "Edit bookmark button is only clickable in the applied search state"
        }

        log.debug {
            "onEditBookmarkClicked(): edit_bookmark_clicked"
        }
    }

    fun onBookmarkChipClicked(item: SearchBookmarkItem) {
        check(stateSubject.value is State.ConfiguringSearch) {
            "Bookmark chips are clickable only in the search configuration state"
        }

        log.debug {
            "onBookmarkChipClicked(): chip_clicked:" +
                    "\nitem=$item"
        }

        selectedBookmark.value = item
    }

    fun onBookmarkChipEditClicked(item: SearchBookmarkItem) {
        check(stateSubject.value is State.ConfiguringSearch) {
            "Bookmark chip edit buttons are clickable only in the search configuration state"
        }

        log.debug {
            "onBookmarkChipEditClicked(): chip_edit_clicked:" +
                    "\nitem=$item"
        }

        eventsSubject.onNext(Event.OpenBookmarkDialog(bookmark = item.source))
    }

    sealed interface State {
        object NoSearch : State
        class ConfiguringSearch(val alreadyAppliedSearch: AppliedGallerySearch?) : State
        class AppliedSearch(val search: AppliedGallerySearch) : State
    }

    sealed interface Event {
        class OpenBookmarkDialog(val bookmark: SearchBookmark?) : Event
    }
}