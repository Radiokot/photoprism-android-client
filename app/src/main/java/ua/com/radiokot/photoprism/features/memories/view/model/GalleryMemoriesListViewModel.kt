package ua.com.radiokot.photoprism.features.memories.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository

class GalleryMemoriesListViewModel(
    private val memoriesRepository: MemoriesRepository,
) : ViewModel() {
    private val log = kLogger("GalleryMemoriesListVM")

    val itemsList = MutableLiveData<List<MemoryListItem>>()
    val isListVisible = MutableLiveData(false)

    /**
     * To be set from the outside whenever
     * displaying of the memories list is required.
     */
    var isListRequired: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateListVisibility()
            }
        }

    init {
        subscribeToRepository()

        itemsList.observeForever {
            updateListVisibility()
        }

        isListVisible.observeForever {
            log.debug {
                "init(): list_visibility_changed:" +
                        "\nisListVisible=$it"
            }
        }

        memoriesRepository.update()
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
        val shouldBeVisible = !itemsList.value.isNullOrEmpty() && isListRequired
        if (shouldBeVisible != isListVisible.value) {
            isListVisible.value = shouldBeVisible
        }
    }

    fun onMemoryItemClicked(item: MemoryListItem) {
        log.debug {
            "onMemoryItemClicked(): memory_item_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            // TODO: Open the viewer and mark the memory as seen.
        }
    }
}
