package ua.com.radiokot.photoprism.api.upload.service

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import ua.com.radiokot.photoprism.api.upload.model.PhotoPrismUploadOptions
import java.io.IOException

interface PhotoPrismUploadService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @POST("v1/users/{userId}/upload/{uploadToken}")
    fun uploadUserFiles(
        @Path("userId")
        userId: String,
        @Path("uploadToken")
        uploadToken: String,
        @Part("files")
        filesPart: MultipartBody.Part,
    )

    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @PUT("v1/users/{userId}/upload/{uploadToken}")
    fun processUserUpload(
        @Path("userId")
        userId: String,
        @Path("uploadToken")
        uploadToken: String,
        @Body
        uploadOptions: PhotoPrismUploadOptions,
    )
}
