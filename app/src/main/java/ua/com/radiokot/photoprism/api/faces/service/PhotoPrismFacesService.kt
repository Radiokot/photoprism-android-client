package ua.com.radiokot.photoprism.api.faces.service

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import ua.com.radiokot.photoprism.api.faces.model.PhotoPrismFace
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import java.io.IOException

interface PhotoPrismFacesService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/faces")
    fun getFaces(
        @Query("count")
        count: Int,
        @Query("offset")
        offset: Int,
        @Query("order")
        order: PhotoPrismOrder,
        @Query("unknown")
        unknown: Boolean,
        @Query("markers")
        markers: Boolean,
        @Query("q")
        q: String? = null,
    ): List<PhotoPrismFace>
}
