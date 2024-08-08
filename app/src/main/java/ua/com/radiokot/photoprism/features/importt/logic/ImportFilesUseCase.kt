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

class ImportFilesUseCase(
    private val contentResolver: ContentResolver,
    private val photoPrismSessionService: PhotoPrismSessionService,
    private val photoPrismUploadService: PhotoPrismUploadService,
) {
    /**
     * Uploads given [files] to the library import and triggers their index.
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
            })
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
    ): Observable<Status.Uploading> = Observable.create { emitter ->
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
}
