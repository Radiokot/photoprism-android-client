package ua.com.radiokot.photoprism.features.widgets.photoframe.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape

class PhotoFrameWidgetConfigurationViewModel(
    private val defaultSearchConfig: SearchConfig,
    defaultShape: PhotoFrameWidgetShape,
): ViewModel() {
    private val log = kLogger("PhotoFrameWidgetConfigurationVM")

    val selectedShape = MutableLiveData(defaultShape)

    fun onShapeClicked(shape: PhotoFrameWidgetShape) {
        log.debug {
            "onShapeClicked: selecting:" +
                    "\nshape=$shape"
        }

        selectedShape.postValue(shape)
    }
}
