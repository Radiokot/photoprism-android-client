package ua.com.radiokot.photoprism.features.gallery.logic

class PhotoPrismThumbnailUrlFactory(
    apiUrl: String,
    private val previewToken: String,
) : MediaThumbnailUrlFactory {
    private val previewUrlBase = apiUrl.trimEnd('/') + "/v1/t"

    override fun getSmallThumbnailUrl(hash: String): String =
        "$previewUrlBase/$hash/$previewToken/tile_224"

    override fun getMediumThumbnailUrl(hash: String): String =
        "$previewUrlBase/$hash/$previewToken/tile_500"
}