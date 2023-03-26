package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository

class SearchBookmarkDialogViewModel(
    private val bookmarksRepository: SearchBookmarksRepository,
) : ViewModel() {
    private val log = kLogger("SearchBookmarkDialogVM")

    private var isInitialized = false

    private val stateSubject = BehaviorSubject.create<State>()
    val state: Observable<State> = stateSubject
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject

    val name = MutableLiveData<String>()
    val isSaveButtonEnabled = MutableLiveData(false)
    val nameMaxLength = NAME_MAX_LENGTH

    private val canSave: Boolean
        get() = !name.value.isNullOrBlank()
                && (name.value?.length ?: 0) <= nameMaxLength

    fun initOnce(bookmark: SearchBookmark?) {
        if (isInitialized) {
            return
        }

        if (bookmark != null) {
            name.value = bookmark.name
            stateSubject.onNext(State.Editing(bookmark))
        } else {
            stateSubject.onNext(State.Creating)
        }

        name.observeForever {
            isSaveButtonEnabled.value = canSave
        }

        log.debug {
            "initOnce(): initialized:" +
                    "\nbookmark=$bookmark"
        }
    }

    fun onSaveButtonClicked() {
        if (canSave) {
            save()
        }
    }

    fun onNameSubmitted(): Boolean {
        return if (canSave) {
            save()
            false
        } else {
            true
        }
    }

    private fun save() {
        val name = name.value!!.trim()

        when (val state = stateSubject.value!!) {
            State.Creating ->
                createNew(
                    name = name,
                )
            is State.Editing ->
                updateExisting(
                    bookmark = state.bookmark,
                    name = name,
                )
        }

    }

    private fun createNew(name: String) {
        log.debug {
            "createNew(): create_new_bookmark:" +
                    "\nname=$name"
        }
    }

    private fun updateExisting(bookmark: SearchBookmark, name: String) {
        log.debug {
            "updateExisting(): update_existing_bookmark:" +
                    "\nname=$name," +
                    "\nbookmark=$bookmark"
        }

        bookmarksRepository.update(bookmark.copy(name = name))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                log.debug {
                    "updateExisting(): existing_bookmark_updated:" +
                            "\nbookmark=$bookmark"
                }

                eventsSubject.onNext(Event.Dismiss)
            }
            .addToCloseables(this)
    }

    fun onDeleteButtonClicked() {
        delete()
    }

    private fun delete() {
        val editingState = (stateSubject.value as? State.Editing)
            .checkNotNull { "Can only delete in the editing state" }
        val bookmark = editingState.bookmark

        log.debug {
            "delete(): deleting:" +
                    "\nbookmark=$bookmark"
        }

        bookmarksRepository.delete(bookmark)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                log.debug {
                    "delete(): bookmark_deleted:" +
                            "\nbookmark=$bookmark"
                }

                eventsSubject.onNext(Event.Dismiss)
            }
            .addToCloseables(this)
    }

    sealed interface State {
        object Creating : State
        class Editing(val bookmark: SearchBookmark) : State
    }

    sealed interface Event {
        object Dismiss : Event
    }

    private companion object {
        private const val NAME_MAX_LENGTH = 30
    }
}