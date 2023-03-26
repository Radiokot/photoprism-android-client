package ua.com.radiokot.photoprism.features.gallery.view.model

import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig

data class AppliedGallerySearch(
    val config: SearchConfig,
    val bookmark: SearchBookmark?,
)