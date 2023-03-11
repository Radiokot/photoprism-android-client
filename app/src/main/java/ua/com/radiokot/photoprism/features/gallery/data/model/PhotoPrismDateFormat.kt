package ua.com.radiokot.photoprism.features.gallery.data.model

import java.text.SimpleDateFormat
import java.util.*

val photoPrismDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    .apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }