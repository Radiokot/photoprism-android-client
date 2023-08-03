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

    override fun getPreview720Url(hash: String): String =
        getFitPreviewUrl(hash, 720)

    override fun getPreview1280Url(hash: String): String =
        getFitPreviewUrl(hash, 1280)

    override fun getPreview1920Url(hash: String): String =
        getFitPreviewUrl(hash, 1920)

    override fun getPreview2048Url(hash: String): String =
        getFitPreviewUrl(hash, 2048)

    override fun getPreview2560Url(hash: String): String =
        getFitPreviewUrl(hash, 2560)

    override fun getPreview3840Url(hash: String): String =
        getFitPreviewUrl(hash, 3840)

    override fun getPreview4096Url(hash: String): String =
        getFitPreviewUrl(hash, 4096)

    override fun getPreview7680Url(hash: String): String =
        getFitPreviewUrl(hash, 7680)

    private fun getFitPreviewUrl(hash: String, size: Int) =
        "$previewUrlBase/t/$hash/$previewToken/fit_$size"

    override fun getMp4PreviewUrl(hash: String): String =
        "$previewUrlBase/videos/$hash/$previewToken/avc"
}
