package ua.com.radiokot.photoprism.view.model.search

import androidx.lifecycle.MutableLiveData

/**
 * A view model which controls the SearchView.
 */
interface SearchViewViewModel: SearchInputViewModel {
    /**
     * Whether the SearchView is expanded or not.
     */
    val isSearchExpanded: MutableLiveData<Boolean>

    fun onSearchIconClicked()
    fun onSearchCloseClicked()
    fun closeAndClearSearch()
}
