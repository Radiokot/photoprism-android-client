package ua.com.radiokot.photoprism.features.gallery.view.model

import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig

sealed class AppliedGallerySearch {
    abstract val config: SearchConfig

    class Custom(override val config: SearchConfig) : AppliedGallerySearch()

    class Bookmarked(val bookmark: SearchBookmark) : AppliedGallerySearch() {
        override val config: SearchConfig = bookmark.searchConfig
    }
}