package ua.com.radiokot.photoprism.features.importt.logic

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.source
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.api.upload.model.PhotoPrismUploadOptions
import ua.com.radiokot.photoprism.api.upload.service.PhotoPrismUploadService
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.toSingle

class ImportFilesUseCase(
    private val files: Collection<Uri>,
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
        val bodyBuilder = MultipartBody.Builder()

        files.forEach { uri ->
            bodyBuilder.addFormDataPart(
                name = "files",
                filename = "file.jpg", // TODO use proper name
                body = object : RequestBody() {
                    override fun contentType(): MediaType? =
                        contentResolver.getType(uri)
                            ?.toMediaTypeOrNull()

                    @SuppressLint("Recycle")
                    override fun writeTo(sink: BufferedSink) {
                        val fileSource = contentResolver.openInputStream(uri)
                            .checkNotNull { "Can't open input stream for $uri" }
                            .source()

                        try {
                            sink.writeAll(fileSource)
                        } finally {
                            fileSource.closeQuietly()
                        }
                    }
                }
            )
        }

        photoPrismUploadService.uploadUserFiles(
            userId = userId,
            uploadToken = uploadToken,
            multipartBody = bodyBuilder.build(),
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
            files: Collection<Uri>,
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
