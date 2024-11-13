package ua.com.radiokot.photoprism.features.gallery.data.model

sealed interface Viewable {
    interface AsImage : Viewable
    interface AsVideo : Viewable
}
