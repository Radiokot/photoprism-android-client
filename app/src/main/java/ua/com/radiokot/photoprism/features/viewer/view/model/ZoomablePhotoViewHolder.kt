package ua.com.radiokot.photoprism.features.viewer.view.model

import com.github.chrisbanes.photoview.PhotoView

interface ZoomablePhotoViewHolder {
    val photoView: PhotoView
    val isScaled: Boolean
        get() = photoView.scale != 1f
}
