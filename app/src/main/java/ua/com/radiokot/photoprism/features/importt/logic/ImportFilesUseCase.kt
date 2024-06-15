package ua.com.radiokot.photoprism.features.importt.logic

import android.content.ContentResolver
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MultipartBody
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.api.upload.model.PhotoPrismUploadOptions
import ua.com.radiokot.photoprism.api.upload.service.PhotoPrismUploadService
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile
import ua.com.radiokot.photoprism.features.importt.model.ImportableFileRequestBody

/**
 * Uploads given [files] to the library import and triggers their index.
 *
 * @param uploadToken a random string used to identify the upload.
 */
class ImportFilesUseCase(
    private val files: List<ImportableFile>,
    private val uploadToken: String,
    private val contentResolver: ContentResolver,
    private val photoPrismSessionService: PhotoPrismSessionService,
    private val photoPrismUploadService: PhotoPrismUploadService,
) {
    private lateinit var userId: String

    operator fun invoke(): Completable =
        getUserId()
            .doOnSuccess { userId = it }
            .flatMapCompletable { uploadFiles() }
            .andThen(processUploadedFiles())

    private fun getUserId(): Single<String> = {
        photoPrismSessionService.getCurrentSession()
            .user
            .uid
    }.toSingle().subscribeOn(Schedulers.io())

    private fun uploadFiles(): Completable = {
        val fileParts = files.map { file ->
            MultipartBody.Part.createFormData(
                name = PhotoPrismUploadService.UPLOAD_FILES_PART_NAME,
                filename = file.displayName,
                body = ImportableFileRequestBody(
                    importableFile = file,
                    contentResolver = contentResolver,
                )
            )
        }

        photoPrismUploadService.uploadUserFiles(
            userId = userId,
            uploadToken = uploadToken,
            files = fileParts,
        )
    }.toCompletable()

    private fun processUploadedFiles(): Completable = {
        photoPrismUploadService.processUserUpload(
            userId = userId,
            uploadToken = uploadToken,
            uploadOptions = PhotoPrismUploadOptions(
                albums = emptyList(), // TODO set albums
            )
        )
    }.toCompletable()

    class Factory(
        private val contentResolver: ContentResolver,
        private val photoPrismSessionService: PhotoPrismSessionService,
        private val photoPrismUploadService: PhotoPrismUploadService,
    ) {
        fun get(
            files: List<ImportableFile>,
            uploadToken: String,
        ) = ImportFilesUseCase(
            files = files,
            uploadToken = uploadToken,
            contentResolver = contentResolver,
            photoPrismSessionService = photoPrismSessionService,
            photoPrismUploadService = photoPrismUploadService,
        )
    }
}
