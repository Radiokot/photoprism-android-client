package ua.com.radiokot.photoprism.env.data.model

import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSession

data class EnvSession(
    var id: String,
    val envConnectionParams: EnvConnectionParams,
    var previewToken: String,
    var downloadToken: String,
) {
    constructor(
        photoPrismSession: PhotoPrismSession,
        envConnectionParams: EnvConnectionParams,
    ) : this(
        id = photoPrismSession.id,
        previewToken = photoPrismSession.config.previewToken,
        downloadToken = photoPrismSession.config.downloadToken,
        envConnectionParams = envConnectionParams,
    )

    companion object {
        fun public(envConnectionParams: EnvConnectionParams) = EnvSession(
            id = "",
            previewToken = "public",
            downloadToken = "public",
            envConnectionParams = envConnectionParams,
        )
    }
}
