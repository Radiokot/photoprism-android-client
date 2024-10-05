package ua.com.radiokot.photoprism.features.ext.memories.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.ext.memories.data.model.Memory
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.ext.memories.view.MemoriesNotificationsManager
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import java.util.concurrent.TimeUnit

class GalleryMemoriesListViewModel(
    private val memoriesRepository: MemoriesRepository,
    private val memoriesNotificationsManager: MemoriesNotificationsManager,
) : ViewModel() {
    private val log = kLogger("GalleryMemoriesListVM")

    val itemsList = MutableLiveData<List<MemoryListItem>>()
    val isViewVisible = MutableLiveData(false)
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    private var isInitialized: Boolean = false
    private var memoryToDelete: Memory? = null

    /**
     * To be set from the outside whenever
     * displaying of the memories list is required.
     */
    var isViewRequired: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateListVisibility()
            }
        }

    fun initOnce() {
        if (isInitialized) {
            return
        }

        subscribeToRepository()

        itemsList.observeForever {
            updateListVisibility()
        }

        memoriesRepository.update()
        memoriesNotificationsManager.cancelNewMemoriesNotification()

        isInitialized = true

        log.debug {
            "initOnce(): initialized"
        }
    }

    private fun subscribeToRepository() {
        memoriesRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { memories ->
                itemsList.value = memories.map(::MemoryListItem)
            }
            .autoDispose(this)

        memoriesRepository.errors
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepository(): memories_loading_failed"
                }
            }
            .autoDispose(this)
    }

    private fun updateListVisibility() {
        val shouldBeVisible = !itemsList.value.isNullOrEmpty() && isViewRequired
        if (shouldBeVisible != isViewVisible.value) {
            isViewVisible.value = shouldBeVisible
        }
    }

    fun onMemoryItemClicked(item: MemoryListItem) {
        log.debug {
            "onMemoryItemClicked(): memory_item_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            val memory = item.source

            eventsSubject.onNext(
                Event.OpenViewer(
                    repositoryParams = SimpleGalleryMediaRepository.Params(
                        query = memory.searchQuery,
                    ),
                    staticSubtitle = MemoryTitle.forMemory(memory),
                )
            )

            if (!memory.isSeen) {
                // Mark the memory as seen with a slight delay
                // to avoid card move while the viewer is opening.
                Completable.timer(500, TimeUnit.MILLISECONDS, Schedulers.io())
                    .andThen(memoriesRepository.markAsSeen(memory))
                    .subscribeBy()
                    .autoDispose(this)
            }

            memoriesNotificationsManager.cancelNewMemoriesNotification()
        }
    }

    fun onMemoryItemLongClicked(item: MemoryListItem) {
        log.debug {
            "onMemoryItemLongClicked(): memory_item_long_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            memoryToDelete = item.source
            eventsSubject.onNext(Event.OpenDeletingConfirmationDialog)
        }
    }

    fun onDeletingConfirmed() {
        val memoryToDelete = memoryToDelete.checkNotNull {
            "Confirming deletion when there's no memory to delete"
        }

        memoriesRepository.delete(memoryToDelete)
            .subscribeBy()
            .autoDispose(this)

        memoriesNotificationsManager.cancelNewMemoriesNotification()
    }

    sealed interface Event {
        class OpenViewer(
            val repositoryParams: SimpleGalleryMediaRepository.Params,
            val staticSubtitle: MemoryTitle,
        ) : Event

        /**
         * Show item deletion confirmation, reporting the choice
         * to the [onDeletingConfirmed] method.
         */
        object OpenDeletingConfirmationDialog : Event
    }
}
