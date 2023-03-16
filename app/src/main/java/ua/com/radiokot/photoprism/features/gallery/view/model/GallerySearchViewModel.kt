package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

class GallerySearchViewModel : ViewModel() {
    private val log = kLogger("GallerySearchViewModel")

    val availableMediaTypes = MutableLiveData(
        listOf(
            GalleryMedia.TypeName.IMAGE,
            GalleryMedia.TypeName.VIDEO,
            GalleryMedia.TypeName.ANIMATED,
            GalleryMedia.TypeName.LIVE,
            GalleryMedia.TypeName.RAW,
            GalleryMedia.TypeName.VECTOR,
        )
    )
    val selectedMediaTypes = MutableLiveData<Set<GalleryMedia.TypeName>>()
    val userQuery = MutableLiveData<CharSequence?>()
    val isApplyButtonEnabled = MutableLiveData(false)
    private val stateSubject = BehaviorSubject.createDefault<State>(State.NoSearch)
    val state: Observable<State> = stateSubject

    init {
        selectedMediaTypes.observeForever {
            isApplyButtonEnabled.value = canApplyConfiguredSearch
        }
        userQuery.observeForever {
            isApplyButtonEnabled.value = canApplyConfiguredSearch
        }
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
                userQuery.value = null

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
            is State.AppliedSearch -> {
                // Expected.
            }
            is State.ConfiguringSearch -> {
                if (state.alreadyAppliedSearch != null) {
                    stateSubject.onNext(State.AppliedSearch(state.alreadyAppliedSearch))
                } else {
                    stateSubject.onNext(State.NoSearch)
                }
            }
            State.NoSearch ->
                throw IllegalStateException()
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
            userQuery = userQuery.value?.toString()?.trim()
        )

        log.debug {
            "applySearch(): applying_search:" +
                    "\nsearch=$search"
        }

        stateSubject.onNext(State.AppliedSearch(search))
    }

    sealed interface State {
        object NoSearch : State
        class ConfiguringSearch(val alreadyAppliedSearch: AppliedGallerySearch?) : State
        class AppliedSearch(val search: AppliedGallerySearch) : State
    }
}