package ua.com.radiokot.photoprism.features.gallery.data.model

import java.io.File

/**
 * A file that can be sent to another app.
 *
 * @param file file stored in the storage, accessible by the app content provider.
 * @param mimeType actual MIME-type of the file disregarding the [file] extension.
 * @param displayName actual name of the file disregarding the [file] name.
 */
data class SendableFile(
    val file: File,
    val mimeType: String,
    val displayName: String,
) {
    constructor(
        downloadedMediaFile: File,
        mediaFile: GalleryMedia.File
    ) : this(
        file = downloadedMediaFile,
        mimeType = mediaFile.mimeType,
        displayName = File(mediaFile.name).name,
    )

    constructor(fileAndDestination: Pair<GalleryMedia.File, File>): this(
        downloadedMediaFile = fileAndDestination.second,
        mediaFile = fileAndDestination.first,
    )
}
