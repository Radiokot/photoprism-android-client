package ua.com.radiokot.photoprism.features.viewer.view

import com.github.chrisbanes.photoview.PhotoView

class ZoomablePhotoView(
    val photoView: PhotoView,
) : ZoomableView {
    override val isZoomed: Boolean
        get() = photoView.scale != 1f

    override fun canPanHorizontally(direction: Int): Boolean = with(photoView) {
        val displayRect = attacher?.displayRect

        if (displayRect == null)
            // Possible due to the attacher nullability.
            false
        else if (direction < 0)
            displayRect.left < left
        else
            displayRect.right > right
    }
}
