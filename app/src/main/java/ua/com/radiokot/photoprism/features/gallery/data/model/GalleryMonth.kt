package ua.com.radiokot.photoprism.features.gallery.data.model

import java.util.*

data class GalleryMonth(
    val firstDay: Date,
    val nextDayAfter: Date,
) : Comparable<GalleryMonth> {

    override fun compareTo(other: GalleryMonth): Int =
        firstDay.compareTo(other.firstDay)
}