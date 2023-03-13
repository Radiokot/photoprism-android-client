package ua.com.radiokot.photoprism.features.gallery.logic

class PhotoPrismMediaFileDownloadUrlFactory(
    apiUrl: String,
    private val downloadToken: String,
) : MediaFileDownloadUrlFactory {
    private val downloadUrlBase = apiUrl.trimEnd('/') + "/v1/dl"

    override fun getDownloadUrl(hash: String): String =
        "$downloadUrlBase/$hash?t=$downloadToken"
}