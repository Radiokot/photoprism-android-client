package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMonth
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Calendar


class GalleryMonthsSequence(
    val startLocalDate: LocalDate,
    val endLocalDate: LocalDate,
) : Sequence<GalleryMonth> {

    init {
        require(startLocalDate <= endLocalDate)
    }

    private val calendar = startLocalDate.getCalendar().apply {
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
            calendar.time < endLocalDate

        override fun next(): GalleryMonth {
            val firstDay = calendar.time

            // Go to next month at this stage.
            calendar.add(Calendar.MONTH, 1)

            return GalleryMonth(
                firstDay = LocalDate(firstDay),
                nextDayAfter = LocalDate(calendar.time),
            )
        }
    }
}
