package ua.com.radiokot.photoprism.features.gallery.logic

class PhotoPrismMediaPreviewUrlFactory(
    apiUrl: String,
    private val previewToken: String,
    private val videoFormatSupport: VideoFormatSupport,
) : MediaPreviewUrlFactory {

    private val previewUrlBase = "${apiUrl}v1"

    override fun getThumbnailUrl(
        thumbnailHash: String,
        sizePx: Int,
    ): String = when {
        sizePx < 180 ->
            getTilePreviewUrl(thumbnailHash, 100)

        sizePx < 400 ->
            getTilePreviewUrl(thumbnailHash, 224)

        else ->
            getTilePreviewUrl(thumbnailHash, 500)
    }

    override fun getImagePreviewUrl(
        previewHash: String,
        sizePx: Int,
    ): String = when {
        sizePx < 1000 ->
            getFitPreviewUrl(previewHash, 720)

        sizePx < 1500 ->
            getFitPreviewUrl(previewHash, 1280)

        sizePx < 2000 ->
            getFitPreviewUrl(previewHash, 1920)

        sizePx < 2500 ->
            getFitPreviewUrl(previewHash, 2048)

        sizePx < 4000 ->
            getFitPreviewUrl(previewHash, 3840)

        sizePx < 4500 ->
            getFitPreviewUrl(previewHash, 4096)

        else ->
            getFitPreviewUrl(previewHash, 7680)
    }

    private fun getTilePreviewUrl(hash: String, size: Int) =
        "$previewUrlBase/t/$hash/$previewToken/tile_$size"

    private fun getFitPreviewUrl(hash: String, size: Int) =
        "$previewUrlBase/t/$hash/$previewToken/fit_$size"

    override fun getVideoPreviewUrl(
        previewHash: String,
        videoFileHash: String?,
        videoFileCodec: String?
    ): String {
        // https://github.com/photoprism/photoprism/blob/2f9792e5411f6bb47a84b638dfc42d51b7790853/frontend/src/model/photo.js#L489

        if (videoFileHash == null || videoFileCodec == null) {
            // Valid case for live photos.
            return "$previewUrlBase/videos/${previewHash}/$previewToken/$DEFAULT_VIDEO_PREVIEW_FORMAT"
        }

        val previewFormat = when {
            // HEIC (live photo) = HEVC is an assumption,
            // but it works for Samsung and Google files.
            // Although some Apple shots may have AVC inside.
            (videoFileCodec == "hvc1" || videoFileCodec == "hev1" || videoFileCodec == "heic")
                    && videoFormatSupport.canPlayHevc() ->
                "hevc"

            videoFileCodec == "vp8" && videoFormatSupport.canPlayVp8() ->
                "vp8"

            videoFileCodec == "vp9" && videoFormatSupport.canPlayVp9() ->
                "vp9"

            (videoFileCodec == "av01" || videoFileCodec == "av1c") && videoFormatSupport.canPlayAv1() ->
                "av01"

            // WebM and OGV seems not supported.

            else ->
                DEFAULT_VIDEO_PREVIEW_FORMAT
        }

        return "$previewUrlBase/videos/${videoFileHash}/$previewToken/$previewFormat"
    }

    private companion object {
        private const val DEFAULT_VIDEO_PREVIEW_FORMAT = "avc"
    }
}
