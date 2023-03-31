package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbum
import java.util.*

data class GalleryMonth(
    val year: Int,
    val monthI: Int,
) : Comparable<GalleryMonth> {
    val end: Date =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthI)

            setOf(
                Calendar.DAY_OF_MONTH,
                Calendar.HOUR_OF_DAY,
                Calendar.MINUTE,
                Calendar.SECOND,
                Calendar.MILLISECOND
            ).forEach {
                set(it, getActualMaximum(it))
            }
        }.time

    init {
        require(monthI in (0..11)) {
            "Month index must be within [0:11]"
        }
    }

    constructor(source: PhotoPrismAlbum) : this(
        year = source.year,
        monthI = source.month - 1,
    )

    override fun compareTo(other: GalleryMonth): Int =
        end.compareTo(other.end)
}