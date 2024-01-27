package ua.com.radiokot.photoprism.features.gallery.data.model

interface WithThumbnail {
    /**
     * @return square thumbnail URL which best fits the required [viewSizePx].
     */
    fun getThumbnailUrl(viewSizePx: Int): String
}
