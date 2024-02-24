package ua.com.radiokot.photoprism.features.memories.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository

class GalleryMemoriesListViewModel(
    private val memoriesRepository: MemoriesRepository,
) : ViewModel() {
    private val log = kLogger("GalleryMemoriesListVM")

    val itemsList = MutableLiveData<List<MemoryListItem>>()
    val isViewVisible = MutableLiveData(false)
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()
    private var isInitialized: Boolean = false

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

            memoriesRepository
                .markAsSeen(memory)
                .subscribeBy()
                .autoDispose(this)
        }
    }

    sealed interface Event {
        class OpenViewer(
            val repositoryParams: SimpleGalleryMediaRepository.Params,
            val staticSubtitle: MemoryTitle,
        ) : Event
    }
}
