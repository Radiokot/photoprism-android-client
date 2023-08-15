package ua.com.radiokot.photoprism.features.gallery.search.view.model

import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig

sealed class AppliedGallerySearch {
    abstract val config: SearchConfig

    data class Custom(override val config: SearchConfig) : AppliedGallerySearch()

    data class Bookmarked(val bookmark: SearchBookmark) : AppliedGallerySearch() {
        override val config: SearchConfig = bookmark.searchConfig
    }
}
