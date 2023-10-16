package ua.com.radiokot.photoprism.features.gallery.logic

import android.media.MediaFormat
import android.os.Build
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import ua.com.radiokot.photoprism.extension.tryOrNull

class MediaCodecVideoFormatSupport : VideoFormatSupport {
    override fun canPlayHevc() =
        isDecodingSupported(MediaFormat.MIMETYPE_VIDEO_HEVC)

    override fun canPlayVp8() =
        isDecodingSupported(MediaFormat.MIMETYPE_VIDEO_VP8)

    override fun canPlayVp9() =
        isDecodingSupported(MediaFormat.MIMETYPE_VIDEO_VP9)

    override fun canPlayAv1(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && isDecodingSupported(MediaFormat.MIMETYPE_VIDEO_AV1)

    private fun isDecodingSupported(mimeType: String) =
        tryOrNull { MediaCodecUtil.getDecoderInfo(mimeType, false, false) } != null
}
