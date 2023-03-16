package ua.com.radiokot.photoprism.features.gallery.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

class GallerySearchViewModel : ViewModel() {
    private val log = kLogger("GallerySearchViewModel")

    val availableMediaTypes = MutableLiveData(
        listOf(
            GalleryMedia.TypeName.IMAGE,
            GalleryMedia.TypeName.VIDEO,
            GalleryMedia.TypeName.ANIMATED,
            GalleryMedia.TypeName.LIVE,
            GalleryMedia.TypeName.RAW,
            GalleryMedia.TypeName.VECTOR,
        )
    )
    val selectedMediaTypes = MutableLiveData<Set<GalleryMedia.TypeName>>()

    fun onAvailableMediaTypeClicked(typeName: GalleryMedia.TypeName) {
        val currentlySelected = selectedMediaTypes.value ?: emptySet()
        if (currentlySelected.contains(typeName)) {
            log.debug {
                "onSearchMediaTypeClicked(): unselect:" +
                        "\ntypeName=$typeName"
            }
            selectedMediaTypes.value = currentlySelected - typeName
        } else {
            log.debug {
                "onSearchMediaTypeClicked(): select:" +
                        "\ntypeName=$typeName"
            }
            selectedMediaTypes.value = currentlySelected + typeName
        }
    }
}