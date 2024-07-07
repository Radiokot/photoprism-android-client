package ua.com.radiokot.photoprism.features.gallery.search.people.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.search.people.data.model.Person
import ua.com.radiokot.photoprism.features.gallery.search.people.data.storage.PeopleRepository
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModel
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModelImpl

class GallerySearchPeopleSelectionViewModel(
    private val peopleRepository: PeopleRepository,
    private val searchPredicate: (person: Person, query: String) -> Boolean,
) : ViewModel(),
    SearchViewViewModel by SearchViewViewModelImpl() {

    private val log = kLogger("GallerySearchPeopleSelectionVM")
    private var isInitialized: Boolean = false

    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()

    /**
     * Non-null set of the selected person IDs, **empty** if nothing is selected.
     */
    private val selectedPersonIds = MutableLiveData<Set<String>>(emptySet())
    private var initialSelectedPersonIds: Set<String>? = null

    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<GallerySearchPersonListItem>>()
    val mainError = MutableLiveData<Error?>(null)
    val isDoneButtonVisible = MutableLiveData(false)
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPressed()
    }

    init {
        subscribeToRepository()
        subscribeToSearch()
        subscribeToPeopleSelection()

        update()
    }

    fun initOnce(currentlySelectedPersonIds: Set<String>) {
        if (isInitialized) {
            return
        }

        val initialSelection = currentlySelectedPersonIds.toSet()
        initialSelectedPersonIds = initialSelection
        selectedPersonIds.value = initialSelection

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\ncurrentlySelectedPeopleCount=${initialSelection.size}"
        }
    }

    fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            peopleRepository.update()
        } else {
            peopleRepository.updateIfNotFresh()
        }
    }

    private fun subscribeToRepository() {
        peopleRepository.items
            .filter { !peopleRepository.isNeverUpdated }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postPeopleItems() }
            .autoDispose(this)

        peopleRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepository(): people_loading_failed"
                }

                if (itemsList.value == null) {
                    mainError.value = Error.LoadingFailed
                } else {
                    eventsSubject.onNext(Event.ShowFloatingLoadingFailedError)
                }
            }
            .autoDispose(this)

        peopleRepository.loading
            .subscribe(isLoading::postValue)
            .autoDispose(this)
    }

    private fun subscribeToSearch() {
        searchInputObservable
            // Only react to the albums are loaded.
            .filter { itemsList.value != null }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postPeopleItems() }
            .autoDispose(this)
    }

    private fun subscribeToPeopleSelection() {
        selectedPersonIds.observeForever { selectedPersonIds ->
            if (itemsList.value != null) {
                postPeopleItems()
            }

            isDoneButtonVisible.value = selectedPersonIds != initialSelectedPersonIds
        }
    }

    private fun postPeopleItems() {
        val repositoryPeople = peopleRepository.itemsList
        val searchQuery = currentSearchInput
        val filteredRepositoryPeople =
            if (searchQuery != null)
                repositoryPeople.filter { person ->
                    searchPredicate(person, searchQuery)
                }
            else
                repositoryPeople
        val selectedPersonIds = selectedPersonIds.value!!
        val hasAnyNames = repositoryPeople.any(Person::hasName)

        log.debug {
            "postPeopleItems(): posting_items:" +
                    "\npeopleCount=${repositoryPeople.size}," +
                    "\nselectedPeopleCount=${selectedPersonIds.size}," +
                    "\nsearchQuery=$searchQuery," +
                    "\nfilteredPeopleCount=${filteredRepositoryPeople.size}"
        }

        itemsList.value =
            filteredRepositoryPeople.map { person ->
                GallerySearchPersonListItem(
                    source = person,
                    isPersonSelected = person.id in selectedPersonIds,
                    isNameShown = hasAnyNames,
                )
            }

        mainError.value =
            when {
                filteredRepositoryPeople.isEmpty() ->
                    Error.NothingFound

                else ->
                    // Dismiss the main error when there are items.
                    null
            }
    }

    fun onPersonItemClicked(item: GallerySearchPersonListItem) {
        log.debug {
            "onPersonItemClicked(): person_item_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            val id = item.source.id
            val currentlySelectedPersonIds = selectedPersonIds.value!!

            if (id in currentlySelectedPersonIds) {
                log.debug {
                    "onPersonItemClicked(): unselect:" +
                            "\npersonId=$id"
                }
                selectedPersonIds.value = currentlySelectedPersonIds - id
            } else {
                log.debug {
                    "onPersonItemClicked(): select:" +
                            "\npersonId=$id"
                }
                selectedPersonIds.value = currentlySelectedPersonIds + id
            }
        }
    }

    fun onRetryClicked() {
        log.debug {
            "onRetryClicked(): updating"
        }

        update(force = true)
    }

    fun onSwipeRefreshPulled() {
        log.debug {
            "onSwipeRefreshPulled(): force_updating"
        }

        update(force = true)
    }

    private fun onBackPressed() {
        log.debug {
            "onBackPressed(): handling_back_press"
        }

        when {
            isSearchExpanded.value == true -> {
                closeAndClearSearch()
            }

            else -> {
                log.debug {
                    "onBackPressed(): finishing_without_result"
                }

                eventsSubject.onNext(Event.Finish)
            }
        }
    }

    fun onDoneClicked() {
        log.debug {
            "onDoneClicked(): finishing_with_result"
        }

        eventsSubject.onNext(Event.FinishWithResult(selectedPersonIds.value!!))
    }

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         * Retry is possible: the [onRetryClicked] method should be called.
         */
        object ShowFloatingLoadingFailedError : Event

        /**
         * Set an OK result with the [selectedPersonIds] and finish.
         */
        class FinishWithResult(
            val selectedPersonIds: Set<String>,
        ) : Event

        /**
         * Finish without setting a result.
         */
        object Finish : Event
    }

    sealed interface Error {
        /**
         * The loading is failed and could be retried.
         * The [onRetryClicked] method should be called.
         */
        object LoadingFailed : Error

        /**
         * No people found for the filter or there are simply no people.
         */
        object NothingFound : Error
    }
}
