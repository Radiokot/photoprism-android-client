package ua.com.radiokot.photoprism.extension

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

fun getUtcCalendar(): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone("UTC"))

fun getUtcCalendar(time: Date): Calendar =
    getUtcCalendar().also { it.time = time }
