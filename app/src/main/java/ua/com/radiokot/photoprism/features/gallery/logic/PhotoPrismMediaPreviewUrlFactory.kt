@file:Suppress("NOTHING_TO_INLINE")

package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.env.data.model.EnvSession

class PhotoPrismMediaPreviewUrlFactory(
    private val session: EnvSession,
    private val videoFormatSupport: VideoFormatSupport,
) : MediaPreviewUrlFactory {

    private val previewUrlBase = "${session.envConnectionParams.apiUrl}v1"
    private val previewToken: String by session::previewToken

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
        viewWidthPx: Int,
        viewHeightPx: Int,
    ): String = when {
        /*
            Name        Width   Height
            ==========================
            fit_720     720     720
            fit_1280    1280	1024
            fit_1920	1920	1200
            fit_2560	2560	1600
            fit_4096	4096	4096
            fit_5120	5120	5120
            fit_7680	7680	4320
         */
        viewWidthPx <= 720 * PREVIEW_SIZE_THRESHOLD_FACTOR
                && viewHeightPx <= 720 * PREVIEW_SIZE_THRESHOLD_FACTOR ->
            getFitPreviewUrl(previewHash, 720)

        viewWidthPx <= 1280 * PREVIEW_SIZE_THRESHOLD_FACTOR
                && viewHeightPx <= 1024 * PREVIEW_SIZE_THRESHOLD_FACTOR ->
            getFitPreviewUrl(previewHash, 1280)

        viewWidthPx <= 1920 * PREVIEW_SIZE_THRESHOLD_FACTOR
                && viewHeightPx <= 1200 * PREVIEW_SIZE_THRESHOLD_FACTOR ->
            getFitPreviewUrl(previewHash, 1920)

        viewWidthPx <= 2560 * PREVIEW_SIZE_THRESHOLD_FACTOR
                && viewHeightPx <= 1600 * PREVIEW_SIZE_THRESHOLD_FACTOR ->
            getFitPreviewUrl(previewHash, 2560)

        viewWidthPx <= 4096 * PREVIEW_SIZE_THRESHOLD_FACTOR
                && viewHeightPx <= 4096 * PREVIEW_SIZE_THRESHOLD_FACTOR ->
            getFitPreviewUrl(previewHash, 4096)

        viewWidthPx <= 5120 * PREVIEW_SIZE_THRESHOLD_FACTOR
                && viewHeightPx <= 5120 * PREVIEW_SIZE_THRESHOLD_FACTOR ->
            getFitPreviewUrl(previewHash, 5120)

        else ->
            getFitPreviewUrl(previewHash, 7680)
    }

    private inline fun getTilePreviewUrl(hash: String, size: Int) =
        "$previewUrlBase/t/$hash/$previewToken/tile_$size"

    private inline fun getFitPreviewUrl(hash: String, size: Int) =
        "$previewUrlBase/t/$hash/$previewToken/fit_$size"

    override fun getVideoPreviewUrl(
        previewHash: String,
        videoFileHash: String?,
        videoFileCodec: String?,
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
        private const val PREVIEW_SIZE_THRESHOLD_FACTOR = 1.3f
    }
}
