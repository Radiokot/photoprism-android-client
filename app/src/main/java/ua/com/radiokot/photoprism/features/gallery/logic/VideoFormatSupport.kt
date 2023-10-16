package ua.com.radiokot.photoprism.features.gallery.logic

interface VideoFormatSupport {
    fun canPlayHevc(): Boolean
    fun canPlayVp8(): Boolean
    fun canPlayVp9(): Boolean
    fun canPlayAv1(): Boolean
}
