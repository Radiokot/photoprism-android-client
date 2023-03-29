package ua.com.radiokot.photoprism.extension

import android.content.Context
import ua.com.radiokot.photoprism.R
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat

/**
 * Loads months string array from the resources and sets it to the current format.
 * Usable if you want to format dates with nominative cased month names ("Ноябрь").
 */
fun SimpleDateFormat.useMonthsFromResources(context: Context) = apply {
    dateFormatSymbols = (dateFormatSymbols.clone() as DateFormatSymbols).apply {
        months = context.resources.getStringArray(R.array.months)
    }
}