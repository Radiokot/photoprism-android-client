package ua.com.radiokot.photoprism.features.gallery.view.model

import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMonth
import ua.com.radiokot.photoprism.util.LocalDate

data class GalleryMonthScrollBubble(
    val localDate: LocalDate,
    val withYear: Boolean,
    val source: GalleryMonth?
)
