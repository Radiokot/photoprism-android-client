@file:Suppress("DEPRECATION")
@file:UnstableApi

package ua.com.radiokot.photoprism.features.viewer.logic

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MediaSourceFactory
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import ua.com.radiokot.photoprism.extension.kLogger

/**
 * A [MediaSourceFactory] decorator applying a fix for [ClippingMediaSource]s with reflection.
 * It allows playing and clipping media the player identifies as unseekable for some reason.
 *
 * Without this fix, live photos taken with Samsung S24 or Fold do not play in fade end viewer.
 */
private class ClippingSourceFixMediaSourceFactory(
    private val delegate: MediaSourceFactory,
) : MediaSourceFactory {

    private val log = kLogger("ClippingSourceFixMSF")

    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider,
    ): MediaSource.Factory =
        delegate.setDrmSessionManagerProvider(drmSessionManagerProvider)

    override fun setLoadErrorHandlingPolicy(
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    ): MediaSource.Factory =
        delegate.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

    override fun getSupportedTypes(): IntArray =
        delegate.supportedTypes

    override fun createMediaSource(
        mediaItem: MediaItem,
    ): MediaSource {
        val source = delegate.createMediaSource(mediaItem)

        if (source is ClippingMediaSource) {
            try {
                // In androidx.media3.exoplayer.source.ClippingMediaSource
                // private final boolean allowUnseekableMedia;
                with(source::class.java.getDeclaredField("allowUnseekableMedia")) {
                    isAccessible = true
                    set(source, true)
                    isAccessible = false
                }

                log.debug {
                    "createMediaSource(): applied_clipping_fix"
                }
            } catch (e: Exception) {
                log.error(e) {
                    "createMediaSource(): failed_applying_clipping_fix"
                }
            }
        }

        return source
    }
}

fun MediaSourceFactory.fixClippingSources(): MediaSourceFactory =
    ClippingSourceFixMediaSourceFactory(
        delegate = this,
    )
