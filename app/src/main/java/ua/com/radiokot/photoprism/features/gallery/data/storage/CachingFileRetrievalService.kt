package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.content.Context
import android.net.Uri
import android.util.Size
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.data.model.CachedMediaItemRecord
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.text.substringAfterLast

class CachingFileRetrievalService(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val cachedMediaItemDao: CachedMediaItemDao,
    private val urlFactory: MediaPreviewUrlFactory,
    private val preferences: GalleryPreferences
) {
    init {
        preferences.clearCache
            .observeOn(Schedulers.io())
            .subscribe {
                clearCache()
            }
    }
    private val log = kLogger("CachingFileRetrievalSvc")

    private val cacheDir by lazy {
        File(context.filesDir, "photo_cache").apply { mkdirs() }
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
                        // look for file in db
                        val cachedMediaItem = cachedMediaItemDao.getByHash(file.hash)

                        if (cachedMediaItem != null) {
                            file.cachedPath =
                                Uri.fromFile(File(cachedMediaItem.localPath)).toString()
                            cachedMediaItemDao.updateLastHit(file.hash, System.currentTimeMillis())
                            return@forEach
                        }


                        val filesToCache: MutableList<Pair<String, String>> = mutableListOf()
                        when (file.mediaType) {
                            "video" -> {
                                val remoteUrl = urlFactory.getVideoPreviewUrl(
                                    photo.hash,
                                    file.hash,
                                    file.codec
                                )
                                val localFileName =
                                    "${photo.uid}-${file.hash}.mp4"
                                if (MediaViewerPage.SHOULD_CACHE_VIDEO)
                                    filesToCache.add(Pair(remoteUrl, localFileName))
                            }
                            "image" -> {
                                val remoteUrl = urlFactory.getImagePreviewUrl(
                                    file.hash,
                                    1920 // TODO: make it use the maximum size of the current device or at least add the size to the configuration
                                )
                                val localFileName =
                                    "${photo.uid}-${file.hash}.${file.name.substringAfterLast('.')}"
                                filesToCache.add(Pair(remoteUrl, localFileName))
                            }
                            "live" -> {
                                val remoteUrl = urlFactory.getImagePreviewUrl(
                                    file.hash,
                                    1920 // TODO: make it use the maximum size of the current device or at least add the size to the configuration
                                )
                                val localFileName =
                                    "${photo.uid}-${file.hash}.${file.name.substringAfterLast('.')}"
                                filesToCache.add(Pair(remoteUrl, localFileName))

                                val videoRemoteUrl = urlFactory.getVideoPreviewUrl(
                                    photo.hash,
                                    file.hash,
                                    file.codec,
                                )
                                val videoLocalFileName =
                                    "${photo.uid}-${file.hash}.mp4"
                                if (MediaViewerPage.SHOULD_CACHE_VIDEO)
                                    filesToCache.add(Pair(videoRemoteUrl, videoLocalFileName))

                            }
                        }

                        manageCache(photos)

                        filesToCache.forEach { (remoteUrl, localFileName) ->
                            val localFile = saveAndGetLocalUrl(shouldDownloadFile, remoteUrl, localFileName)

                            // fill in the cached path to point to the local file.
                            if (localFile != null) {
                                // not good for live as video and image has same hash / file record
                                file.cachedPath = Uri.fromFile(localFile).toString()
                                // add new entry to db
                                cachedMediaItemDao.insert(
                                    CachedMediaItemRecord(
                                        hash = file.hash,
                                        localPath = localFile.path,
                                        cachedAt = System.currentTimeMillis(), // current time
                                        size = localFile.length(),
                                        lastHitAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        log.error(e) { "cacheAndAssignLocalPaths(): failed to cache file: file=$file" }
                        // If caching fails, the original remote URL remains,
                        // so the app can still try to load it from the network.
                    }
                }
        }
    }

    private fun manageCache(photos: Sequence<PhotoPrismMergedPhoto>) {
        // check that the cache is not too big
        val cacheSizeInBytes = cachedMediaItemDao.getCacheSizeInBytes()
        if ((cacheSizeInBytes / 1000000) > (preferences.cacheSizeLimitInMb.value ?: 0)
        ) {
            // remove old cache files
            var sizeToRemove =
                cacheSizeInBytes - (preferences.cacheSizeLimitInMb.value ?: 0) * 1000000
            for (record in cachedMediaItemDao.getAllByLastHit()) {
                val file = File(record.localPath)
                if (file.exists()) {
                    file.delete()
                    log.debug { "cacheAndAssignLocalPaths(): deleted file: localFile=$file" }
                }
                cachedMediaItemDao.delete(record)
                // remove the cachedpath from any photos in our list
                val selectedPhoto = photos.firstOrNull { it.files.any { it.hash == record.hash } }
                if (selectedPhoto != null) {
                    val selectedFile = selectedPhoto.files.first { it.hash == record.hash }
                    selectedFile.cachedPath = null
                }
                sizeToRemove -= record.size
                if (sizeToRemove <= 0)
                    break
            }
        }
    }

    private fun saveAndGetLocalUrl(
        shouldDownloadFile: Boolean,
        remoteUrl: String,
        targetFileName: String
    ): File? {
        // Use UID and file hash to create a unique local filename.
        val localFile = File(
            cacheDir,
            targetFileName
        )

        if (!localFile.exists()) {
            if (shouldDownloadFile) {
                log.debug { "cacheAndAssignLocalPaths(): downloading: remoteUrl=$remoteUrl, localFile=$localFile" }
                downloadFile(remoteUrl, localFile)
                return localFile
            }
            return null
        } else {
            log.debug { "cacheAndAssignLocalPaths(): already cached: localFile=$localFile" }
            return localFile
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

    fun clearCache() {
        try {
            cachedMediaItemDao.getAllByLastHit().forEach { record ->
                val file = File(record.localPath)
                if (file.exists() && !file.delete()) {
                    log.warn { "clearCache(): failed to delete cached file: ${record.localPath}" }
                }
                cachedMediaItemDao.delete(record)
            }
        } catch (e: Exception) {
            log.error(e) { "clearCache(): failed to clear cache" }
        }
    }
}
