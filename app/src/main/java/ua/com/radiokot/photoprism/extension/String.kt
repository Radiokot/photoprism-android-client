package ua.com.radiokot.photoprism.extension

import java.util.Locale

/**
 * Replace of the deprecated [capitalize] method.
 */
fun String.capitalized(locale: Locale = Locale.getDefault()) =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
