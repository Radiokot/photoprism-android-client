package ua.com.radiokot.photoprism.features.gallery.data.model

import java.text.SimpleDateFormat
import java.util.*

val photoPrismDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    .apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

fun parsePhotoPrismDate(date: String): Date? = synchronized(photoPrismDateFormat) {
    photoPrismDateFormat.parse(date)
}

fun formatPhotoPrismDate(date: Date): String = synchronized(photoPrismDateFormat) {
    photoPrismDateFormat.format(date)
}