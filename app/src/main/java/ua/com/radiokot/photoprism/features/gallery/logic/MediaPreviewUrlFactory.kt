package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

interface MediaPreviewUrlFactory {
    fun getThumbnailUrl(
        thumbnailHash: String,
        sizePx: Int,
    ): String

    fun getImagePreviewUrl(
        previewHash: String,
        sizePx: Int,
    ): String

    fun getVideoPreviewUrl(
        previewHash: String,
        videoFileHash: String?,
        videoFileCodec: String?,
    ): String

    fun getVideoPreviewUrl(
        galleryMedia: GalleryMedia,
    ): String {
        val videoFile = galleryMedia.videoFile

        return getVideoPreviewUrl(
            previewHash = galleryMedia.hash,
            videoFileHash = videoFile?.hash,
            videoFileCodec = videoFile?.codec,
        )
    }
}
