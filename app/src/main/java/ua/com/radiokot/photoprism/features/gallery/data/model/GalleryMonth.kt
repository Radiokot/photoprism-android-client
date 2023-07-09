package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.util.LocalDate

data class GalleryMonth(
    val firstDay: LocalDate,
    val nextDayAfter: LocalDate,
) : Comparable<GalleryMonth> {

    override fun compareTo(other: GalleryMonth): Int =
        firstDay.compareTo(other.firstDay)
}
