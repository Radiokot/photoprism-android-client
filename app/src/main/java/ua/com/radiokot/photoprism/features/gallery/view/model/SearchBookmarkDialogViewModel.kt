package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark

class SearchBookmarkDialogViewModel : ViewModel() {
    private val log = kLogger("SearchBookmarkDialogVM")

    private var isInitialized = false
    val name = MutableLiveData<String>()
    val isSaveButtonEnabled = MutableLiveData(false)
    val isDeleteButtonVisible = MutableLiveData(false)
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
            isDeleteButtonVisible.value = true
        } else {
            isDeleteButtonVisible.value = false
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

        log.debug {
            "save(): saving:" +
                    "\nname=$name"
        }
    }

    private companion object {
        private const val NAME_MAX_LENGTH = 30
    }
}