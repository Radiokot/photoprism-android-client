package ua.com.radiokot.photoprism.api.upload.service

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import ua.com.radiokot.photoprism.api.upload.model.PhotoPrismUploadOptions
import java.io.IOException

interface PhotoPrismUploadService {
    /**
     * @param userId session user ID (`useibtXXXX`)
     * @param uploadToken a random string which is later used to [processUserUpload]
     * @param files one or more parts named [UPLOAD_FILES_PART_NAME] with the content to upload.
     */
    @kotlin.jvm.Throws(IOException::class)
    @Multipart
    @Headers("Accept: application/json")
    @POST("v1/users/{userId}/upload/{uploadToken}")
    fun uploadUserFiles(
        @Path("userId")
        userId: String,
        @Path("uploadToken")
        uploadToken: String,
        @Part
        files: List<MultipartBody.Part>,
    ): Any

    /**
     * @param userId session user ID (`useibtXXXX`)
     * @param uploadToken a random string used to [uploadUserFiles]
     */
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
    ): Any

    companion object {
        const val UPLOAD_FILES_PART_NAME = "files"
    }
}
