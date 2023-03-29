package ua.com.radiokot.photoprism.extension

import java.util.*

fun Date.isSameDayAs(other: Date): Boolean {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val thisYear = calendar[Calendar.YEAR]
    val thisDay = calendar[Calendar.DAY_OF_YEAR]
    calendar.time = other
    val otherYear = calendar[Calendar.YEAR]
    val otherDay = calendar[Calendar.DAY_OF_YEAR]

    return thisYear == otherYear && thisDay == otherDay
}

fun Date.isSameMonthAs(other: Date): Boolean {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val thisYear = calendar[Calendar.YEAR]
    val thisMonth = calendar[Calendar.MONTH]
    calendar.time = other
    val otherYear = calendar[Calendar.YEAR]
    val otherMonth = calendar[Calendar.MONTH]

    return thisYear == otherYear && thisMonth == otherMonth
}

fun Date.isSameYearAs(other: Date): Boolean {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val thisYear = calendar[Calendar.YEAR]
    calendar.time = other
    val otherYear = calendar[Calendar.YEAR]

    return thisYear == otherYear
}