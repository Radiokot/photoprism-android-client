package ua.com.radiokot.photoprism.api.photos.service

import retrofit2.http.GET
import retrofit2.http.Query
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismPhoto

interface PhotoPrismPhotosService {
    @GET("v1/photos")
    fun getPhotos(
        @Query("count")
        count: Int,
        @Query("offset")
        offset: Int,
        @Query("merged")
        merged: Boolean = true,
        @Query("order")
        order: PhotoPrismOrder = PhotoPrismOrder.NEWEST,
        @Query("public")
        public: Boolean = true,
        @Query("q")
        q: String? = null
    ): List<PhotoPrismPhoto>
}