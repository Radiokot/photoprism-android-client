package ua.com.radiokot.photoprism.features.memories.data.model

import ua.com.radiokot.photoprism.features.gallery.data.model.WithThumbnail
import ua.com.radiokot.photoprism.features.gallery.data.model.WithThumbnailFromUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import java.util.Date

sealed class Memory(
    /**
     * PhotoPrism search query to fetch the memory contents.
     */
    val searchQuery: String,
    /**
     * Creation date used to determine how long a particular memory is kept.
     */
    val createdAt: Date,
    /**
     * Whether the user have seen this memory.
     */
    var isSeen: Boolean,
    val previewHash: String,
    previewUrlFactory: MediaPreviewUrlFactory,
) : WithThumbnail by WithThumbnailFromUrlFactory(previewHash, previewUrlFactory) {

    /**
     * "This day N years ago" – few photos or videos taken this day in the past [year].
     */
    class ThisDayInThePast(
        /**
         * Past year to which this memory is referred to.
         */
        val year: Int,
        searchQuery: String,
        createdAt: Date,
        isSeen: Boolean,
        previewHash: String,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : Memory(searchQuery, createdAt, isSeen, previewHash, previewUrlFactory) {
        constructor(
            year: Int,
            searchQuery: String,
            previewHash: String,
            previewUrlFactory: MediaPreviewUrlFactory,
        ) : this(
            year = year,
            searchQuery = searchQuery,
            createdAt = Date(),
            isSeen = false,
            previewHash = previewHash,
            previewUrlFactory = previewUrlFactory,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Memory) return false

        if (searchQuery != other.searchQuery) return false

        return true
    }

    override fun hashCode(): Int {
        return searchQuery.hashCode()
    }
}
