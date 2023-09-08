package ua.com.radiokot.photoprism.features.gallery.search.view.model

import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.core.Observable

/**
 * A view model which controls the SearchView.
 */
interface SearchViewViewModel {
    /**
     * Raw non-null SearchView input.
     */
    val rawSearchInput: MutableLiveData<String>

    /**
     * Whether the SearchView is expanded or not.
     */
    val isSearchExpanded: MutableLiveData<Boolean>

    /**
     * Filtered and debounced [Observable] from [rawSearchInput]
     */
    val searchInputObservable: Observable<String>

    /**
     * Filtered current [rawSearchInput] value.
     * Null if the raw input is empty.
     */
    val currentSearchInput: String?

    fun onSearchIconClicked()
    fun onSearchCloseClicked()
    fun closeAndClearSearch()
}
