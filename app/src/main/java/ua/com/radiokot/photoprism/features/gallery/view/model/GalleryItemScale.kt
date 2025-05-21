package ua.com.radiokot.photoprism.features.gallery.view.model

import ua.com.radiokot.photoprism.R

enum class GalleryItemScale(
    /**
     * Multiplier for the default list item min size
     *
     * @see R.dimen.list_item_gallery_media_min_size
     */
    val factor: Float,
) {
    TINY(0.5f),
    SMALL(0.75f),
    NORMAL(1f),
    LARGE(1.5f),
    HUGE(2f),
    ;
}
