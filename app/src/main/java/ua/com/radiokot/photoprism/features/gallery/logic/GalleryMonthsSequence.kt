package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMonth
import java.util.*


class GalleryMonthsSequence(
    val startDate: Date,
    val endDate: Date,
) : Sequence<GalleryMonth> {

    init {
        require(startDate <= endDate)
    }

    private val calendar = Calendar.getInstance().apply {
        time = startDate

        // Set to the beginning of the month.
        setOf(
            Calendar.DAY_OF_MONTH,
            Calendar.HOUR_OF_DAY,
            Calendar.MINUTE,
            Calendar.SECOND,
            Calendar.MILLISECOND
        ).forEach {
            set(it, getActualMinimum(it))
        }
    }

    override fun iterator() = object : Iterator<GalleryMonth> {
        override fun hasNext(): Boolean =
            calendar.time < endDate

        override fun next(): GalleryMonth {
            val firstDay = calendar.time

            // Go to next month at this stage.
            calendar.add(Calendar.MONTH, 1)

            return GalleryMonth(
                firstDay = firstDay,
                nextDayAfter = calendar.time,
            )
        }
    }
}