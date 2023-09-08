package ua.com.radiokot.photoprism.features.gallery.search.view.model

import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.extension.kLogger
import java.util.concurrent.TimeUnit

class SearchViewViewModelImpl : SearchViewViewModel {
    private val log = kLogger("SearchViewVM")

    override val rawSearchInput: MutableLiveData<String> = MutableLiveData("")
    override val isSearchExpanded: MutableLiveData<Boolean> = MutableLiveData(false)
    override val searchInputObservable: Observable<String>
        get() = Observable
            .create { emitter ->
                rawSearchInput.observeForever(emitter::onNext)
            }
            .distinctUntilChanged()
            .debounce { value ->
                // Apply debounce to the input unless it is empty (input is cleared).
                if (value.isEmpty())
                    Observable.just(0L)
                else
                    Observable.timer(400, TimeUnit.MILLISECONDS)
            }
    override val currentSearchInput: String?
        get() = rawSearchInput.value?.takeIf(String::isNotEmpty)

    override fun onSearchIconClicked() {
        if (isSearchExpanded.value != true) {
            log.debug {
                "onSearchIconClicked(): expanding_search"
            }

            isSearchExpanded.value = true
        }
    }

    override fun onSearchCloseClicked() {
        if (isSearchExpanded.value != false) {
            log.debug {
                "onSearchCloseClicked(): closing_search"
            }

            closeAndClearSearch()
        }
    }

    override fun closeAndClearSearch() {
        // Because of the SearchView internal logic, order matters.
        // First clear, then collapse. Otherwise it won't collapse.
        rawSearchInput.value = ""
        isSearchExpanded.value = false

        log.debug {
            "closeAndClearSearch(): closed_and_cleared"
        }
    }
}
