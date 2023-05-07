@file:Suppress("DEPRECATION")
// Will find alternative reliable ISO8601 date parser if removed.
// Can't use single SimpleDateFormat because PhotoPrism dates has optional milliseconds.

package ua.com.radiokot.photoprism.features.gallery.data.model

import com.fasterxml.jackson.databind.util.ISO8601Utils
import java.text.ParsePosition
import java.util.*

fun parsePhotoPrismDate(date: String): Date? =
    ISO8601Utils.parse(date, ParsePosition(0))

fun formatPhotoPrismDate(date: Date): String =
    ISO8601Utils.format(date)