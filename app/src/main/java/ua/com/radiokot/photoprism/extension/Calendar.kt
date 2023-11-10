package ua.com.radiokot.photoprism.extension

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

fun getUtcCalendar(): Calendar =
    GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"))

fun getUtcCalendar(time: Date): Calendar =
    getUtcCalendar().also { it.time = time }
