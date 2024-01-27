package ua.com.radiokot.photoprism.features.gallery.data.model

interface ViewableAsImage {
    /**
     * @param viewSizePx size of the view in which the preview will be shown.
     * If the view is zoomable, the size should be the max possible one.
     *
     * @return image preview URL (compressed & resized original)
     * which best fits the required [viewSizePx].
     */
    fun getImagePreviewUrl(viewSizePx: Int): String
}
