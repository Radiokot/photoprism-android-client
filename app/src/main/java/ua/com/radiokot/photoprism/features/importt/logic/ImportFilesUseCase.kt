package ua.com.radiokot.photoprism.features.importt.logic

import android.content.ContentResolver
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MultipartBody
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.api.upload.model.PhotoPrismUploadOptions
import ua.com.radiokot.photoprism.api.upload.service.PhotoPrismUploadService
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.importt.albums.data.model.ImportAlbum
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile
import ua.com.radiokot.photoprism.features.importt.model.ImportableFileRequestBody

/**
 * Uploads given [files] to the library import and triggers their index.
 *
 * @param uploadToken a random string used to identify the upload.
 */
class ImportFilesUseCase(
    private val files: List<ImportableFile>,
    private val albums: Set<ImportAlbum>,
    private val uploadToken: String,
    private val contentResolver: ContentResolver,
    private val photoPrismSessionService: PhotoPrismSessionService,
    private val photoPrismUploadService: PhotoPrismUploadService,
) {
    init {
        require(files.isNotEmpty()) {
            "Files can't be empty"
        }
    }

    private lateinit var userId: String

    operator fun invoke(): Observable<Status> =
        Observable.just<Status>(Status.Uploading.INDETERMINATE)
            .concatWith(
                getUserId()
                    .doOnSuccess { userId = it }
                    .map { Status.Uploading.INDETERMINATE }
            )
            .concatWith(uploadFiles())
            .concatWith(Observable.just(Status.ProcessingUpload))
            .concatWith(processUploadedFiles())

    private fun getUserId(): Single<String> = {
        photoPrismSessionService.getCurrentSession()
            .user
            .uid
    }.toSingle().subscribeOn(Schedulers.io())

    private fun uploadFiles(): Observable<Status.Uploading> = Observable.create { emitter ->
        val progressPerFile = DoubleArray(files.size)
        val fileParts = files.mapIndexed { fileIndex, file ->
            MultipartBody.Part.createFormData(
                name = PhotoPrismUploadService.UPLOAD_FILES_PART_NAME,
                filename = file.displayName,
                body = ImportableFileRequestBody(
                    importableFile = file,
                    contentResolver = contentResolver,
                    onReadingProgress = { bytesRead: Long ->
                        progressPerFile[fileIndex] =
                            if (file.size > 0)
                                (bytesRead.toDouble() / file.size) * 100
                            else
                                100.0

                        emitter.onNext(
                            Status.Uploading(
                                percent = progressPerFile.average()
                            )
                        )
                    }
                )
            )
        }

        emitter.onNext(Status.Uploading.INDETERMINATE)

        photoPrismUploadService.uploadUserFiles(
            userId = userId,
            uploadToken = uploadToken,
            files = fileParts,
        )

        emitter.onComplete()
    }.subscribeOn(Schedulers.io())

    private fun processUploadedFiles(): Completable = {
        photoPrismUploadService.processUserUpload(
            userId = userId,
            uploadToken = uploadToken,
            uploadOptions = PhotoPrismUploadOptions(
                albums = albums.map { album->
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

    class Factory(
        private val contentResolver: ContentResolver,
        private val photoPrismSessionService: PhotoPrismSessionService,
        private val photoPrismUploadService: PhotoPrismUploadService,
    ) {
        fun get(
            files: List<ImportableFile>,
            albums: Set<ImportAlbum>,
            uploadToken: String,
        ) = ImportFilesUseCase(
            files = files,
            albums=albums,
            uploadToken = uploadToken,
            contentResolver = contentResolver,
            photoPrismSessionService = photoPrismSessionService,
            photoPrismUploadService = photoPrismUploadService,
        )
    }
}
