package ua.com.radiokot.photoprism.features.importt.logic

import android.content.ContentResolver
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.kotlin.toObservable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MultipartBody
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.api.upload.model.PhotoPrismUploadOptions
import ua.com.radiokot.photoprism.api.upload.service.PhotoPrismUploadService
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.retryWithDelay
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.importt.albums.data.model.ImportAlbum
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile
import ua.com.radiokot.photoprism.features.importt.model.ImportableFileRequestBody
import ua.com.radiokot.photoprism.features.shared.albums.data.storage.AlbumsRepository
import java.util.concurrent.TimeUnit

/**
 * @param albumsRepository to be updated on success if creating albums
 */
class ImportFilesUseCase(
    private val contentResolver: ContentResolver,
    private val photoPrismSessionService: PhotoPrismSessionService,
    private val photoPrismUploadService: PhotoPrismUploadService,
    private val albumsRepository: AlbumsRepository?,
) {
    private val log = kLogger("ImportFilesUseCase")

    /**
     * Uploads given [files] to the library import and triggers their index.
     * Updates [AlbumsRepository] on success if there are albums to create.
     *
     * @param uploadToken a random string used to identify the upload.
     */
    operator fun invoke(
        files: List<ImportableFile>,
        albums: Set<ImportAlbum>,
        uploadToken: String,
    ): Observable<Status> {
        require(files.isNotEmpty()) {
            "Files can't be empty"
        }

        lateinit var userId: String

        return Observable.just<Status>(Status.Uploading.INDETERMINATE)
            .concatWith(
                getUserId()
                    .doOnSubscribe {
                        log.debug {
                            "invoke(): getting_user_id_started"
                        }
                    }
                    .doOnSuccess { userId = it }
                    .map { Status.Uploading.INDETERMINATE }
            )
            .concatWith(Observable.defer {
                uploadFiles(
                    files = files,
                    userId = userId,
                    uploadToken = uploadToken
                )
            })
            .concatWith(Observable.just(Status.ProcessingUpload))
            .concatWith(Completable.defer {
                processUploadedFiles(
                    albums = albums,
                    userId = userId,
                    uploadToken = uploadToken,
                )
                    .doOnSubscribe {
                        log.debug {
                            "invoke(): processing_upload_started"
                        }
                    }
                    .retryWithDelay(
                        times = MAX_RETRIES,
                        delay = RETRY_DELAY_S,
                        unit = TimeUnit.SECONDS,
                    )
            })
            .doOnComplete {
                // Update albums repository if there are albums to create,
                // so they are available in subsequent imports.
                if (albums.any { it is ImportAlbum.ToCreate }) {
                    albumsRepository?.invalidate()
                    albumsRepository?.updateIfEverUpdated()

                    log.debug {
                        "invoke(): albums_invalidated"
                    }
                }

                log.debug {
                    "invoke(): completed"
                }
            }
    }

    private fun getUserId(): Single<String> = {
        photoPrismSessionService.getCurrentSession()
            .user
            .uid
    }.toSingle().subscribeOn(Schedulers.io())

    private fun uploadFiles(
        files: List<ImportableFile>,
        userId: String,
        uploadToken: String,
    ): Observable<Status.Uploading> {
        val progressPerFile = DoubleArray(files.size)

        return files
            .mapIndexed { fileIndex, file ->
                fileIndex to file
            }
            .toObservable()
            // Use flatMap with maxConcurrency to limit the number
            // of concurrent uploads.
            .flatMap({ (fileIndex, file) ->
                uploadSingleFile(
                    file = file,
                    userId = userId,
                    uploadToken = uploadToken
                )
                    .map { bytesRead ->
                        val updatedProgress =
                            if (file.size > 0)
                                (bytesRead.toDouble() / file.size) * 100
                            else
                                100.0

                        if (updatedProgress > progressPerFile[fileIndex]) {
                            progressPerFile[fileIndex] = updatedProgress
                        }

                        Status.Uploading(
                            percent = progressPerFile.average()
                        )
                    }
                    .doOnSubscribe {
                        log.debug {
                            "uploadFiles(): single_file_upload_started:" +
                                    "\nfile=$file"
                        }
                    }
                    .retryWithDelay(
                        times = MAX_RETRIES,
                        delay = RETRY_DELAY_S,
                        unit = TimeUnit.SECONDS,
                    )
                    .doOnComplete {
                        log.debug {
                            "uploadFiles(): single_file_upload_done:" +
                                    "\nfile=$file"
                        }
                    }
            }, MAX_CONCURRENT_UPLOADS)
    }

    private fun uploadSingleFile(
        file: ImportableFile,
        userId: String,
        uploadToken: String,
    ): Observable<Long> = Observable.create { emitter ->
        val part = MultipartBody.Part.createFormData(
            name = PhotoPrismUploadService.UPLOAD_FILES_PART_NAME,
            filename = file.displayName,
            body = ImportableFileRequestBody(
                importableFile = file,
                contentResolver = contentResolver,
                onReadingProgress = emitter::onNext,
            )
        )

        photoPrismUploadService.uploadUserFiles(
            userId = userId,
            uploadToken = uploadToken,
            files = listOf(part),
        )

        emitter.onComplete()
    }.subscribeOn(Schedulers.io())

    private fun processUploadedFiles(
        albums: Set<ImportAlbum>,
        userId: String,
        uploadToken: String,
    ): Completable = {
        photoPrismUploadService.processUserUpload(
            userId = userId,
            uploadToken = uploadToken,
            uploadOptions = PhotoPrismUploadOptions(
                albums = albums.map { album ->
                    when (album) {
                        is ImportAlbum.Existing ->
                            album.uid

                        is ImportAlbum.ToCreate ->
                            album.title
                    }
                },
            )
        )
    }.toCompletable().subscribeOn(Schedulers.io())

    sealed interface Status {
        /**
         * Uploading files.
         *
         * @param percent progress percent from 0 to 100 or -1 for indeterminate.
         */
        class Uploading(
            val percent: Double,
        ) : Status {
            companion object {
                val INDETERMINATE = Uploading(percent = -1.0)
            }
        }

        /**
         * Processing uploaded files.
         */
        object ProcessingUpload : Status
    }

    private companion object {
        private const val MAX_CONCURRENT_UPLOADS = 4
        private const val MAX_RETRIES = 6
        private const val RETRY_DELAY_S = 10L
    }
}
