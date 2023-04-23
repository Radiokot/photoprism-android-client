package ua.com.radiokot.photoprism.api.photos.service

import retrofit2.http.GET
import retrofit2.http.Query
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import java.io.IOException

interface PhotoPrismPhotosService {
    @kotlin.jvm.Throws(IOException::class)
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
}