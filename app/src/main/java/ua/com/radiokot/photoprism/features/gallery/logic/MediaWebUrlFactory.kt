package ua.com.radiokot.photoprism.features.gallery.logic

interface MediaWebUrlFactory {
    fun getWebViewUrl(uid: String): String
}