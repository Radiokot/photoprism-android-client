package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.content.Context
import android.net.Uri
import android.util.Size
import okhttp3.OkHttpClient
import okhttp3.Request
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.text.substringAfterLast

class CachingFileRetrievalService(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val urlFactory: MediaPreviewUrlFactory
) {
    private val log = kLogger("CachingFileRetrievalSvc")
    private val cacheDir by lazy {
        File(context.cacheDir, "photo_cache").apply { mkdirs() }
    }

    /**
     * Downloads files for the given photos and updates their links to point to local files.
     */
    fun cacheAndAssignCachePaths(
        photos: Sequence<PhotoPrismMergedPhoto>,
        shouldDownloadFile: Boolean
    ) {
        // we should take the size from the displayer but it's not really available here, we'll hard code to 1920 for now

        photos.forEach { photo ->
            photo.files
//                .filter { it.isImageMimeType } // We only cache displayable images.
                .forEach { file ->
                    // cache image files
                    try {
                        var remoteUrl: String? = null
                        var localFileName: String
                        if (file.video == true) {
                            remoteUrl = urlFactory.getVideoPreviewUrl( photo.hash,
                                file.hash,
                                file.codec)
                            localFileName =
                                "${photo.uid}-${file.hash}.video}";
                        } else {
                            remoteUrl = urlFactory.getImagePreviewUrl(
                                file.hash,
                                1920 // TODO: make it use the maximum size of the current device or at least add the size to the configuration
                            )
                            localFileName =
                                "${photo.uid}-${file.hash}.${file.name.substringAfterLast('.')}";
                        }

                        val localFile =
                            saveAndGetLocalUrl(shouldDownloadFile, remoteUrl, localFileName)

                        // fill in the cached path to point to the local file.
                        file.cachedPath = Uri.fromFile(localFile).toString()

                    } catch (e: Exception) {
                        log.error(e) { "cacheAndAssignLocalPaths(): failed to cache file: file=$file" }
                        // If caching fails, the original remote URL remains,
                        // so the app can still try to load it from the network.
                    }
                }
        }
    }

    private fun saveAndGetLocalUrl(
        shouldDownloadFile: Boolean,
        remoteUrl: String,
        targetFileName: String
    ): File {
        // Use UID and file hash to create a unique local filename.
        val localFile = File(
            cacheDir,
            targetFileName
        )

        if (!localFile.exists()) {
            if (shouldDownloadFile) {
                log.debug { "cacheAndAssignLocalPaths(): downloading: remoteUrl=$remoteUrl, localFile=$localFile" }
                downloadFile(remoteUrl, localFile)
            }
        } else {
            log.debug { "cacheAndAssignLocalPaths(): already cached: localFile=$localFile" }
        }
        return localFile
    }

    private fun downloadFile(url: String, destination: File) {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download file: ${response.code} from $url")
            }
            response.body?.byteStream()?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Response body is null for $url")
        }
    }

    /**
     * Checks whether a file in the cache exists for the current file, fills in the cachedpath of each file if the file exists and downloads the file into the cache otherwise
     */
    fun cacheAndAssignCachePaths(photos: Sequence<PhotoPrismMergedPhoto>) {
        cacheAndAssignCachePaths(photos, true)
    }

    /**
     * Checks whether a file in the cache exists for the current file, fills in the cachedpath of each file if the file exists and does nothing otherwise
     */
    fun checkForCache(photos: Sequence<PhotoPrismMergedPhoto>) {
        cacheAndAssignCachePaths(photos, false)
    }
}
