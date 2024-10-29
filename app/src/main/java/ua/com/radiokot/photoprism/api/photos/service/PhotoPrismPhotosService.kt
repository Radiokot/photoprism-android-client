package ua.com.radiokot.photoprism.api.photos.service

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismBatchPhotoUids
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismPhotoUpdate
import java.io.IOException

interface PhotoPrismPhotosService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/photos?merged=true")
    fun getMergedPhotos(
        @Query("count")
        count: Int,
        @Query("offset")
        offset: Int,
        @Query("order")
        order: PhotoPrismOrder = PhotoPrismOrder.NEWEST,
        @Query("public")
        public: Boolean = true,
        @Query("q")
        q: String? = null
    ): List<PhotoPrismMergedPhoto>

    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @POST("v1/batch/photos/archive")
    fun batchArchive(
        @Body batchPhotoUids: PhotoPrismBatchPhotoUids,
    ): Any // There must be some non-void return type because of Retrofit assertions.

    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @POST("v1/batch/photos/delete")
    fun batchDelete(
        @Body batchPhotoUids: PhotoPrismBatchPhotoUids,
    ): Any // There must be some non-void return type because of Retrofit assertions.

    @kotlin.jvm.Throws(IOException::class)
    @JvmSuppressWildcards
    @Headers("Accept: application/json")
    @PUT("v1/photos/{photoUid}")
    fun updatePhoto(
        @Path("photoUid") photoUid: String,
        @Body update: PhotoPrismPhotoUpdate,
    ): Any // There must be some non-void return type because of Retrofit assertions.
}
