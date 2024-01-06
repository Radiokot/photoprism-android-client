package ua.com.radiokot.photoprism.features.memories.data.model

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
    /**
     * Direct URL to the small square static thumbnail
     */
    val smallThumbnailUrl: String,
): Comparable<Memory> {

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
        smallThumbnailUrl: String,
    ) : Memory(searchQuery, createdAt, isSeen, smallThumbnailUrl) {
        constructor(
            year: Int,
            searchQuery: String,
            smallThumbnailUrl: String,
        ) : this(
            year = year,
            searchQuery = searchQuery,
            createdAt = Date(),
            isSeen = false,
            smallThumbnailUrl = smallThumbnailUrl,
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

    override fun compareTo(other: Memory): Int =
        createdAt.compareTo(other.createdAt)
}
