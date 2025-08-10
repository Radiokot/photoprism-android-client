package ua.com.radiokot.photoprism.features.viewer.view

import com.mikepenz.fastadapter.diff.DiffCallback
import ua.com.radiokot.photoprism.features.viewer.view.model.FadeEndLivePhotoViewerPage
import ua.com.radiokot.photoprism.features.viewer.view.model.ImageViewerPage
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage
import ua.com.radiokot.photoprism.features.viewer.view.model.UnsupportedNoticePage
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoViewerPage

class MediaViewerPageDiffCallback : DiffCallback<MediaViewerPage> {

    override fun areItemsTheSame(
        oldItem: MediaViewerPage,
        newItem: MediaViewerPage
    ): Boolean =
        oldItem::class == newItem::class
                && oldItem.identifier == newItem.identifier

    override fun areContentsTheSame(
        oldItem: MediaViewerPage,
        newItem: MediaViewerPage
    ): Boolean = when {

        oldItem is ImageViewerPage
                && newItem is ImageViewerPage ->
            oldItem.imageViewSize == newItem.imageViewSize

        oldItem is FadeEndLivePhotoViewerPage
                && newItem is FadeEndLivePhotoViewerPage ->
            oldItem.imageViewSize == newItem.imageViewSize

        oldItem is VideoViewerPage
                && newItem is VideoViewerPage ->
            oldItem.needsVideoControls == newItem.needsVideoControls

        oldItem is UnsupportedNoticePage
                && newItem is UnsupportedNoticePage ->
            true

        else ->
            false
    }

    override fun getChangePayload(
        oldItem: MediaViewerPage,
        oldItemPosition: Int,
        newItem: MediaViewerPage,
        newItemPosition: Int
    ): Any? = null
}
