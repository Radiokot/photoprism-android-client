package ua.com.radiokot.photoprism.features.shared.search.view.model

import androidx.lifecycle.MutableLiveData
import ua.com.radiokot.photoprism.extension.kLogger

class SearchViewViewModelImpl :
    SearchViewViewModel,
    SearchInputViewModel by SearchInputViewModelImpl() {

    private val log = kLogger("SearchViewVM")

    override val isSearchExpanded: MutableLiveData<Boolean> = MutableLiveData(false)

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
