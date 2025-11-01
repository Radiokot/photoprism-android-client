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
    fun cacheAndAssignCachePaths(photos: Sequence<PhotoPrismMergedPhoto>) {
        // we should take the size from the displayer but it's not really available here, we'll hard code to 1920 for now

        photos.forEach { photo ->
            photo.files
//                .filter { it.isImageMimeType } // We only cache displayable images.
                .forEach { file ->
                    try {
                        val remoteUrl = urlFactory.getImagePreviewUrl(
                            file.hash,
                            1920

//                            max(
//                                imageViewSize.width,
//                                imageViewSize.height
//                        )
                        ) //?: return@forEach



                        // Use UID and file hash to create a unique local filename.
                        val localFile = File(
                            cacheDir,
                            "${photo.uid}-${file.hash}.${file.name.substringAfterLast('.')}"
                        )

                        if (!localFile.exists()) {
                            log.debug { "cacheAndAssignLocalPaths(): downloading: remoteUrl=$remoteUrl, localFile=$localFile" }
                            downloadFile(remoteUrl, localFile)
                        } else {
                            log.debug { "cacheAndAssignLocalPaths(): already cached: localFile=$localFile" }
                        }

                        // Mutate the link to point to the local file.
                        file.cachedPath = Uri.fromFile(localFile).toString()

                    } catch (e: Exception) {
                        log.error(e) { "cacheAndAssignLocalPaths(): failed to cache file: file=$file" }
                        // If caching fails, the original remote URL remains,
                        // so the app can still try to load it from the network.
                    }
                }
        }
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
}
