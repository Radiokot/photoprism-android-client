package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.env.data.model.EnvSession

class PhotoPrismMediaFileDownloadUrlFactory(
    private val session: EnvSession,
) : MediaFileDownloadUrlFactory {
    private val downloadUrlBase = "${session.envConnectionParams.apiUrl}v1/dl"

    override fun getDownloadUrl(hash: String): String =
        "$downloadUrlBase/$hash?t=${session.downloadToken}"
}
