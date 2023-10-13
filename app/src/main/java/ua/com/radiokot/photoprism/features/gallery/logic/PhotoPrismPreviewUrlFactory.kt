package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto

class PhotoPrismPreviewUrlFactory(
    apiUrl: String,
    private val previewToken: String,
    private val videoFormatSupport: VideoFormatSupport,
) : MediaPreviewUrlFactory {
    private val previewUrlBase = "${apiUrl}v1"

    override fun getSmallThumbnailUrl(hash: String): String =
        "$previewUrlBase/t/$hash/$previewToken/tile_224"

    override fun getMediumThumbnailUrl(hash: String): String =
        "$previewUrlBase/t/$hash/$previewToken/tile_500"

    override fun getImagePreview720Url(hash: String): String =
        getFitPreviewUrl(hash, 720)

    override fun getImagePreview1280Url(hash: String): String =
        getFitPreviewUrl(hash, 1280)

    override fun getImagePreview1920Url(hash: String): String =
        getFitPreviewUrl(hash, 1920)

    override fun getImagePreview2048Url(hash: String): String =
        getFitPreviewUrl(hash, 2048)

    override fun getImagePreview2560Url(hash: String): String =
        getFitPreviewUrl(hash, 2560)

    override fun getImagePreview3840Url(hash: String): String =
        getFitPreviewUrl(hash, 3840)

    override fun getImagePreview4096Url(hash: String): String =
        getFitPreviewUrl(hash, 4096)

    override fun getImagePreview7680Url(hash: String): String =
        getFitPreviewUrl(hash, 7680)

    private fun getFitPreviewUrl(hash: String, size: Int) =
        "$previewUrlBase/t/$hash/$previewToken/fit_$size"

    override fun getVideoPreviewUrl(mergedPhoto: PhotoPrismMergedPhoto): String {
        // https://github.com/photoprism/photoprism/blob/2f9792e5411f6bb47a84b638dfc42d51b7790853/frontend/src/model/photo.js#L489

        val file = mergedPhoto.videoFile
        if (file?.width == null || file.height == null) {
            return "$previewUrlBase/videos/${mergedPhoto.hash}/$previewToken/avc"
        }

        val fileCodec = file.codec ?: ""
        val previewFormat = when {
            (fileCodec == "hvc1" || fileCodec == "hev1")
                    && videoFormatSupport.canPlayHevc(file.width, file.height, file.fps) ->
                "hevc"

            fileCodec == "vp8"
                    && videoFormatSupport.canPlayVp8(file.width, file.height, file.fps) ->
                "vp8"

            fileCodec == "vp9"
                    && videoFormatSupport.canPlayVp9(file.width, file.height, file.fps) ->
                "vp9"

            (fileCodec == "av01" || fileCodec == "av1c")
                    && videoFormatSupport.canPlayAv1(file.width, file.height, file.fps) ->
                "av01"

            // WebM and OGV seems not supported.

            else ->
                "avc"
        }

        return "$previewUrlBase/videos/${file.hash}/$previewToken/$previewFormat"
    }
}
