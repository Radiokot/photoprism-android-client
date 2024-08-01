package ua.com.radiokot.photoprism.di

import android.text.format.DateFormat
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.extension.setUtcTimeZone
import java.text.SimpleDateFormat
import java.util.Locale

const val UTC_MONTH_DATE_FORMAT = "utc-month"
const val UTC_MONTH_YEAR_DATE_FORMAT = "utc-month-year"
const val UTC_DAY_DATE_FORMAT = "utc-day"
const val UTC_DAY_YEAR_DATE_FORMAT = "utc-day-year"
const val UTC_DAY_YEAR_SHORT_DATE_FORMAT = "utc-day-year-short"
const val UTC_DATE_TIME_DATE_FORMAT = "utc-date-time"
const val UTC_DATE_TIME_YEAR_DATE_FORMAT = "utc-date-time-year"

val dateFormatModule = module {
    includes(localeModule)

    factory(named(UTC_MONTH_DATE_FORMAT)) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(get(), "MMMM"),
            get<Locale>()
        ).setUtcTimeZone()
    } bind java.text.DateFormat::class

    factory(named(UTC_MONTH_YEAR_DATE_FORMAT)) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(get(), "MMMMyyyy"),
            get<Locale>()
        ).setUtcTimeZone()
    } bind java.text.DateFormat::class

    factory(named(UTC_DAY_DATE_FORMAT)) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(get(), "EEMMMMd"),
            get<Locale>()
        ).setUtcTimeZone()
    } bind java.text.DateFormat::class

    factory(named(UTC_DAY_YEAR_DATE_FORMAT)) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(get(), "EEMMMMdyyyy"),
            get<Locale>()
        ).setUtcTimeZone()
    } bind java.text.DateFormat::class

    factory(named(UTC_DAY_YEAR_SHORT_DATE_FORMAT)) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(get(), "MMMMdyyyy"),
            get<Locale>()
        ).setUtcTimeZone()
    } bind java.text.DateFormat::class

    factory(named(UTC_DATE_TIME_DATE_FORMAT)) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(get(), "EEMMMMd HH:mm"),
            get<Locale>()
        ).setUtcTimeZone()
    } bind java.text.DateFormat::class

    factory(named(UTC_DATE_TIME_YEAR_DATE_FORMAT)) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(get(), "EEMMMMdyyyy HH:mm"),
            get<Locale>()
        ).setUtcTimeZone()
    } bind java.text.DateFormat::class
}
