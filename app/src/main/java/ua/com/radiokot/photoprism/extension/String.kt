package ua.com.radiokot.photoprism.extension

import java.util.Locale

/**
 * Replacement of the deprecated [capitalize] method.
 * Turns the first char into the title case.
 */
fun String.capitalized(locale: Locale = Locale.getDefault()) =
    replaceFirstChar { it.titlecase(locale) }
