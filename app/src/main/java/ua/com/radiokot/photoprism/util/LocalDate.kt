package ua.com.radiokot.photoprism.util

import ua.com.radiokot.photoprism.extension.getUtcCalendar
import ua.com.radiokot.photoprism.features.gallery.data.model.formatPhotoPrismDate
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * A [Date] which represents the local datetime, as if we were in UTC.
 * It explicitly offsets the [Date.getTime] by the local timezone offset.
 *
 * If we are in UTC+2 and the current local time is 13:00,
 * [Date] will have 11:00 but [LocalDate] will have 13:00.
 *
 * All the temporal operations (Calendar, DateFormat) must be done in UTC
 * to get proper result.
 *
 * This workaround is to be deleted once the kotlinx library leaves alpha.
 */
class LocalDate : Date {
    /**
     * Creates an instance with the current time using [localTimeZone] as an UTC offset source.
     */
    constructor(localTimeZone: TimeZone) : super(
        System.currentTimeMillis().let { utcMillis ->
            utcMillis + localTimeZone.getOffset(utcMillis)
        }
    )

    /**
     * Creates an instance wrapping an already UTCfied local time.
     */
    constructor(localTimeMillis: Long): super(localTimeMillis)

    /**
     * Creates an instance wrapping an already UTCfied local date.
     */
    constructor(localDate: Date) : this(localDate.time)

    /**
     * Creates an instance with the current time using the current default timezone
     * as an UTC offset source.
     */
    constructor() : this(localTimeZone = TimeZone.getDefault())


    fun getCalendar(): Calendar =
        getUtcCalendar(time = this)

    fun isSameDayAs(other: LocalDate): Boolean {
        val calendar = getCalendar()
        val thisYear = calendar[Calendar.YEAR]
        val thisDay = calendar[Calendar.DAY_OF_YEAR]
        calendar.time = other
        val otherYear = calendar[Calendar.YEAR]
        val otherDay = calendar[Calendar.DAY_OF_YEAR]

        return thisYear == otherYear && thisDay == otherDay
    }

    fun isSameMonthAs(other: LocalDate): Boolean {
        val calendar = getCalendar()
        val thisYear = calendar[Calendar.YEAR]
        val thisMonth = calendar[Calendar.MONTH]
        calendar.time = other
        val otherYear = calendar[Calendar.YEAR]
        val otherMonth = calendar[Calendar.MONTH]

        return thisYear == otherYear && thisMonth == otherMonth
    }

    fun isSameYearAs(other: LocalDate): Boolean {
        val calendar = getCalendar()
        val thisYear = calendar[Calendar.YEAR]
        calendar.time = other
        val otherYear = calendar[Calendar.YEAR]

        return thisYear == otherYear
    }

    override fun toString(): String =
        "LocalDate(${formatPhotoPrismDate(this)})"
}
