package ua.com.radiokot.photoprism.features.gallery.logic

interface VideoFormatSupport {
    fun canPlayHevc(width: Int, height: Int, fps: Double?): Boolean
    fun canPlayVp8(width: Int, height: Int, fps: Double?): Boolean
    fun canPlayVp9(width: Int, height: Int, fps: Double?): Boolean
    fun canPlayAv1(width: Int, height: Int, fps: Double?): Boolean
}
