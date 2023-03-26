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
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
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

    // All the operations are expected to be almost instant,
    // there is no need for loading.
    // But a simple lock is required anyway.
    private var isBusy = false

    private val canSave: Boolean
        get() = !name.value.isNullOrBlank()
                && (name.value?.length ?: 0) <= nameMaxLength

    fun initOnce(
        searchConfig: SearchConfig,
        existingBookmark: SearchBookmark?
    ) {
        if (isInitialized) {
            return
        }

        if (existingBookmark != null) {
            name.value = existingBookmark.name
            stateSubject.onNext(State.Editing(existingBookmark))
        } else {
            stateSubject.onNext(State.Creating(searchConfig))
        }

        name.observeForever {
            isSaveButtonEnabled.value = canSave
        }

        log.debug {
            "initOnce(): initialized:" +
                    "\nstate=${stateSubject.value}"
        }
    }

    fun onSaveButtonClicked() {
        if (canSave && !isBusy) {
            save()
        }
    }

    fun onNameSubmitted(): Boolean {
        return if (canSave && !isBusy) {
            save()
            false
        } else {
            true
        }
    }

    private fun save() {
        val name = name.value!!.trim()

        when (val state = stateSubject.value!!) {
            is State.Creating ->
                createNew(
                    name = name,
                    searchConfig = state.searchConfig,
                )
            is State.Editing ->
                updateExisting(
                    name = name,
                    bookmark = state.bookmark,
                )
        }
    }

    private fun createNew(
        name: String,
        searchConfig: SearchConfig,
    ) {
        log.debug {
            "createNew(): create_new_bookmark:" +
                    "\nname=$name," +
                    "\nsearchConfig=$searchConfig"
        }

        bookmarksRepository.create(
            name = name,
            searchConfig = searchConfig,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { isBusy = true }
            .doOnTerminate { isBusy = false }
            .subscribeBy(
                onSuccess = { createdBookmark ->
                    log.debug {
                        "createNew(): new_bookmark_created:" +
                                "\ncreatedBookmark=$createdBookmark"
                    }

                    eventsSubject.onNext(Event.Dismiss)
                },
                onError = { error ->
                    log.error(error) { "createNew(): error_occurred" }

                    eventsSubject.onNext(
                        Event.ShowFloatingError(
                            Error.FailedToCreate(
                                shortSummary = error.shortSummary,
                            )
                        )
                    )
                }
            )
            .addToCloseables(this)
    }

    private fun updateExisting(
        name: String,
        bookmark: SearchBookmark,
    ) {
        log.debug {
            "updateExisting(): update_existing_bookmark:" +
                    "\nname=$name," +
                    "\nbookmark=$bookmark"
        }

        bookmarksRepository.update(bookmark.copy(name = name))
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { isBusy = true }
            .doOnTerminate { isBusy = false }
            .subscribeBy(
                onComplete = {
                    log.debug {
                        "updateExisting(): existing_bookmark_updated:" +
                                "\nbookmark=$bookmark"
                    }

                    eventsSubject.onNext(Event.Dismiss)
                },
                onError = { error ->
                    log.error(error) { "updateExisting(): error_occurred" }

                    eventsSubject.onNext(
                        Event.ShowFloatingError(
                            Error.FailedToUpdate(
                                shortSummary = error.shortSummary,
                            )
                        )
                    )
                }
            )
            .addToCloseables(this)
    }

    fun onDeleteButtonClicked() {
        if (!isBusy) {
            delete()
        }
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
            .doOnSubscribe { isBusy = true }
            .doOnTerminate { isBusy = false }
            .subscribeBy(
                onComplete = {
                    log.debug {
                        "delete(): bookmark_deleted:" +
                                "\nbookmark=$bookmark"
                    }

                    eventsSubject.onNext(Event.Dismiss)
                },
                onError = { error ->
                    log.error(error) { "delete(): error_occurred" }

                    eventsSubject.onNext(
                        Event.ShowFloatingError(
                            Error.FailedToDelete(
                                shortSummary = error.shortSummary,
                            )
                        )
                    )
                }
            )
            .addToCloseables(this)
    }

    sealed interface State {
        data class Creating(val searchConfig: SearchConfig) : State
        data class Editing(val bookmark: SearchBookmark) : State
    }

    sealed interface Event {
        object Dismiss : Event
        class ShowFloatingError(val error: Error) : Event
    }

    sealed class Error(val shortSummary: String) {
        class FailedToDelete(shortSummary: String) : Error(shortSummary)
        class FailedToUpdate(shortSummary: String) : Error(shortSummary)
        class FailedToCreate(shortSummary: String) : Error(shortSummary)
    }

    private companion object {
        private const val NAME_MAX_LENGTH = 30
    }
}