package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.data.model.Person
import ua.com.radiokot.photoprism.features.gallery.data.storage.PeopleRepository

class GallerySearchPeopleViewModel(
    private val peopleRepository: PeopleRepository,
) : ViewModel() {
    private val log = kLogger("GallerySearchPeopleVM")
    private val stateSubject = BehaviorSubject.createDefault<State>(State.Loading)
    val state = stateSubject.toMainThreadObservable()

    /**
     * Non-null set of the selected person UIDs, **empty** if nothing is selected.
     */
    val selectedPersonUids = MutableLiveData<Set<String>>(emptySet())

    init {
        subscribeToRepository()
        subscribeToPeopleSelection()
    }

    fun updateIfNotFresh() {
        log.debug {
            "updateIfNotFresh(): begin_loading"
        }

        peopleRepository.updateIfNotFresh()
    }

    private fun subscribeToRepository() {
        peopleRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { people ->
                if (people.isEmpty() && peopleRepository.isNeverUpdated) {
                    stateSubject.onNext(State.Loading)
                } else {
                    postReadyState()
                }
            }
            .autoDispose(this)

        peopleRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepository(): people_loading_failed"
                }

                stateSubject.onNext(State.LoadingFailed)
            }
            .autoDispose(this)
    }

    private fun postReadyState() {
        val repositoryPeople = peopleRepository.itemsList

        val selectedPersonUids = selectedPersonUids.value!!

        log.debug {
            "postReadyState(): posting_ready_state:" +
                    "\npeopleCount=${repositoryPeople.size}," +
                    "\nselectedPeopleCount=${selectedPersonUids.size}"
        }

        val hasAnyNames = repositoryPeople.any(Person::hasName)

        stateSubject.onNext(
            State.Ready(
                people = repositoryPeople.map { person ->
                    PersonListItem(
                        source = person,
                        isPersonSelected = person.uid in selectedPersonUids,
                        isNameShown = hasAnyNames,
                    )
                }
            )
        )
    }

    private fun subscribeToPeopleSelection() {
        selectedPersonUids.observeForever {
            val currentState = stateSubject.value
            if (currentState is State.Ready) {
                postReadyState()
            }
        }
    }

    fun onPersonItemClicked(item: PersonListItem) {
        val currentState = stateSubject.value
        check(currentState is State.Ready) {
            "People are clickable only in the ready state"
        }

        log.debug {
            "onPersonItemClicked(): person_item_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            val uid = item.source.uid
            val currentlySelectedPersonUids = selectedPersonUids.value!!

            if (currentlySelectedPersonUids.contains(uid)) {
                log.debug {
                    "onPersonItemClicked(): unselect:" +
                            "\npersonUid=$uid"
                }
                selectedPersonUids.value = currentlySelectedPersonUids - uid
            } else {
                log.debug {
                    "onPersonItemClicked(): select:" +
                            "\npersonUid=$uid"
                }
                selectedPersonUids.value = currentlySelectedPersonUids + uid
            }
        }
    }

    fun onReloadPeopleClicked() {
        log.debug {
            "onReloadPeopleClicked(): reload_people_clicked"
        }

        updateIfNotFresh()
    }

    fun getPersonThumbnail(uid: String): String? =
        peopleRepository.getLoadedPerson(uid)?.smallThumbnailUrl

    sealed interface State {
        object Loading : State
        class Ready(
            val people: List<PersonListItem>,
        ) : State

        object LoadingFailed : State
    }
}
