package ua.com.radiokot.photoprism.features.gallery.logic

class PhotoPrismMediaDownloadUrlFactory(
    apiUrl: String,
    private val downloadToken: String,
) : MediaFileDownloadUrlFactory {
    private val downloadUrlBase = "${apiUrl}v1/dl"

    override fun getDownloadUrl(hash: String): String =
        "$downloadUrlBase/$hash?t=$downloadToken"
}