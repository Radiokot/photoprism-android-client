package ua.com.radiokot.photoprism.features.gallery.logic

class PhotoPrismPreviewUrlFactory(
    apiUrl: String,
    private val previewToken: String,
) : MediaPreviewUrlFactory {
    private val previewUrlBase = apiUrl.trimEnd('/') + "/v1/t"

    override fun getSmallThumbnailUrl(hash: String): String =
        "$previewUrlBase/$hash/$previewToken/tile_224"

    override fun getMediumThumbnailUrl(hash: String): String =
        "$previewUrlBase/$hash/$previewToken/tile_500"

    override fun getHdPreviewUrl(hash: String): String =
        "$previewUrlBase/$hash/$previewToken/fit_1280"
}