package ua.com.radiokot.photoprism.features.people.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.people.data.model.Person
import ua.com.radiokot.photoprism.features.people.data.storage.PeopleRepository
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModel
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModelImpl

class PeopleSelectionViewModel(
    private val peopleRepository: PeopleRepository,
    private val searchPredicate: (person: Person, query: String) -> Boolean,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) : ViewModel(),
    SearchViewViewModel by SearchViewViewModelImpl() {

    private val log = kLogger("PeopleSelectionVM")
    private var isInitialized: Boolean = false

    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()

    private var allPersonIds: Set<String> = emptySet()

    /**
     * Set of the selected person IDs.
     * **empty** if nothing is selected,
     * **null** if selection is not yet initialized.
     * Initialized in repo subscription.
     */
    private val selectedPersonIds = MutableLiveData<Set<String>>()
    private val notSelectedPersonIds: Set<String>
        get() = allPersonIds - selectedPersonIds.value!!
    private var initialSelectedPersonIds: Set<String>? = null
    private var initialNotSelectedPersonIds: Set<String>? = null

    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<SelectablePersonListItem>>()
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

    fun initOnce(
        currentlySelectedPersonIds: Set<String>?,
        currentlyNotSelectedPersonIds: Set<String>?,
    ) {
        if (isInitialized) {
            return
        }

        initialSelectedPersonIds = currentlySelectedPersonIds?.toSet()
        initialNotSelectedPersonIds = currentlyNotSelectedPersonIds?.toSet()

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\ncurrentlySelectedPeopleCount=${initialSelectedPersonIds?.size}," +
                    "\ncurrentlyNotSelectedPeopleCount=${initialNotSelectedPersonIds?.size}"
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
            .subscribe { people ->

                allPersonIds = people.mapTo(mutableSetOf(), Person::id)

                if (selectedPersonIds.value == null) {
                    // Select requested IDs, if any.
                    initialSelectedPersonIds?.also { personIdsToSelect ->
                        selectedPersonIds.value = personIdsToSelect
                    }

                    // Select everything except requested IDs, if any.
                    initialNotSelectedPersonIds?.also { personIdsToNotSelect ->
                        selectedPersonIds.value = allPersonIds - personIdsToNotSelect
                    }
                }

                postPeopleItems()
            }
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

            isDoneButtonVisible.value = when {
                initialSelectedPersonIds != null ->
                    selectedPersonIds != initialSelectedPersonIds

                initialNotSelectedPersonIds != null ->
                    notSelectedPersonIds != initialNotSelectedPersonIds

                else ->
                    true
            }
        }
    }

    private fun postPeopleItems() {
        val selectedPersonIds = selectedPersonIds.value
        if (selectedPersonIds == null) {
            log.warn {
                "postPeopleItems(): skipping_as_selection_not_yet_set_up"
            }
            return
        }
        val repositoryPeople = peopleRepository.itemsList
        val searchQuery = currentSearchInput
        val filteredRepositoryPeople =
            if (searchQuery != null)
                repositoryPeople.filter { person ->
                    searchPredicate(person, searchQuery)
                }
            else
                repositoryPeople
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
                SelectablePersonListItem(
                    source = person,
                    isPersonSelected = person.id in selectedPersonIds,
                    isNameShown = hasAnyNames,
                    previewUrlFactory = previewUrlFactory,
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

    fun onPersonItemClicked(item: SelectablePersonListItem) {
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

        eventsSubject.onNext(
            Event.FinishWithResult(
                selectedPersonIds = selectedPersonIds.value!!,
                notSelectedPersonIds = notSelectedPersonIds,
            )
        )
    }

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         * Retry is possible: the [onRetryClicked] method should be called.
         */
        object ShowFloatingLoadingFailedError : Event

        /**
         * Set an OK result with the given ID sets and finish.
         */
        class FinishWithResult(
            val selectedPersonIds: Set<String>,
            val notSelectedPersonIds: Set<String>,
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
