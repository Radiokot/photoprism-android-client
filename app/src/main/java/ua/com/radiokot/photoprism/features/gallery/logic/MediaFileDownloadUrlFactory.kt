package ua.com.radiokot.photoprism.features.gallery.logic

interface MediaFileDownloadUrlFactory {
    fun getDownloadUrl(hash: String): String
}