package ua.com.radiokot.photoprism.extension

import java.text.DateFormat
import java.util.TimeZone

fun <T: DateFormat> T.setUtcTimeZone() = apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
