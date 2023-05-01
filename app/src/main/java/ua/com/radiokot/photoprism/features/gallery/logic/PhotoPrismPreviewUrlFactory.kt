package ua.com.radiokot.photoprism.features.gallery.logic

class PhotoPrismPreviewUrlFactory(
    apiUrl: String,
    private val previewToken: String,
) : MediaPreviewUrlFactory {
    private val previewUrlBase = "${apiUrl}v1"

    override fun getSmallThumbnailUrl(hash: String): String =
        "$previewUrlBase/t/$hash/$previewToken/tile_224"

    override fun getMediumThumbnailUrl(hash: String): String =
        "$previewUrlBase/t/$hash/$previewToken/tile_500"

    override fun getHdPreviewUrl(hash: String): String =
        "$previewUrlBase/t/$hash/$previewToken/fit_1280"

    override fun getMp4PreviewUrl(hash: String): String =
        "$previewUrlBase/videos/$hash/$previewToken/avc"
}