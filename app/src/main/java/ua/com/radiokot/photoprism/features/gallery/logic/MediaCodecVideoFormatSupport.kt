package ua.com.radiokot.photoprism.features.gallery.logic

import android.media.MediaFormat
import android.os.Build
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import ua.com.radiokot.photoprism.extension.tryOrNull

class MediaCodecVideoFormatSupport : VideoFormatSupport {
    override fun canPlayHevc(width: Int, height: Int, fps: Double?) =
        isDecodingSupported(
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            width,
            height,
            fps,
        )

    override fun canPlayVp8(width: Int, height: Int, fps: Double?) =
        isDecodingSupported(
            MediaFormat.MIMETYPE_VIDEO_VP8,
            width,
            height,
            fps,
        )

    override fun canPlayVp9(width: Int, height: Int, fps: Double?) =
        isDecodingSupported(
            MediaFormat.MIMETYPE_VIDEO_VP9,
            width,
            height,
            fps,
        )

    override fun canPlayAv1(width: Int, height: Int, fps: Double?): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isDecodingSupported(
            MediaFormat.MIMETYPE_VIDEO_AV1,
            width,
            height,
            fps,
        )

    private fun isDecodingSupported(mimeType: String, width: Int, height: Int, fps: Double?) =
        tryOrNull { MediaCodecUtil.getDecoderInfo(mimeType, false, false) }
            ?.capabilities
            ?.videoCapabilities
            ?.run {
                // Capabilities size support check requires the size to be aligned,
                // which is not always true for videos with custom cropping.
                val closestWidth =
                    if (widthAlignment == 0 || width % widthAlignment == 0)
                        width
                    else
                        widthAlignment * (width / widthAlignment + 1)
                val closestHeight =
                    if (heightAlignment == 0 || height % heightAlignment == 0)
                        height
                    else
                        heightAlignment * (height / heightAlignment + 1)

                if (fps != null)
                    areSizeAndRateSupported(closestWidth, closestHeight, fps)
                else
                    isSizeSupported(closestWidth, closestHeight)
            }
            ?: false
}
