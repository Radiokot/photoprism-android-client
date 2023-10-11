package ua.com.radiokot.photoprism.features.gallery.logic

import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

class MediaCodecListVideoFormatSupport : VideoFormatSupport {
    private val codecs = MediaCodecList(MediaCodecList.ALL_CODECS)

    override fun canPlayHevc(width: Int, height: Int) =
        codecs.hasDecoder(
            MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_HEVC,
                width,
                height,
            )
        )

    override fun canPlayVp8(width: Int, height: Int) =
        codecs.hasDecoder(
            MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_VP8,
                width,
                height,
            )
        )

    override fun canPlayVp9(width: Int, height: Int) =
        codecs.hasDecoder(
            MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_VP9,
                width,
                height
            )
        )

    override fun canPlayAv1(width: Int, height: Int): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && codecs.hasDecoder(
            MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AV1,
                width,
                height
            )
        )

    private fun MediaCodecList.hasDecoder(mediaFormat: MediaFormat): Boolean =
        findDecoderForFormat(mediaFormat) != null
}
