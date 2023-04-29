package ua.com.radiokot.photoprism.features.gallery.logic

interface MediaWebUrlFactory {
    /**
     * @return URL to view a media by the [uid] in the web viewer
     */
    fun getWebViewUrl(uid: String): String
}