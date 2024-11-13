package ua.com.radiokot.photoprism.features.ext.memories.data.model

import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Calendar
import java.util.Date

class Memory(
    /**
     * Type-specific memory data.
     */
    val typeData: TypeData,
    /**
     * PhotoPrism search query to fetch the memory contents.
     */
    val searchQuery: String,
    /**
     * Creation date used to determine how long a particular memory is kept.
     */
    val createdAt: Date = Date(),
    /**
     * Whether the user have seen this memory.
     */
    var isSeen: Boolean = false,
    val thumbnailHash: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Memory) return false

        if (searchQuery != other.searchQuery) return false

        return true
    }

    override fun hashCode(): Int {
        return searchQuery.hashCode()
    }

    override fun toString(): String {
        return "Memory(searchQuery='$searchQuery', type=$typeData)"
    }

    sealed class TypeData {
        /**
         * "This day N years ago" – few photos or videos taken this day in the past [year].
         */
        class ThisDayInThePast(
            /**
             * Past year to which this memory is referred to.
             */
            val year: Int,
        ) : TypeData() {
            /**
             * How many years ago is this memory.
             */
            val yearsAgo: Int = LocalDate().getCalendar()[Calendar.YEAR] - year
        }
    }
}
