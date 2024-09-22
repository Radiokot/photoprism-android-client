package ua.com.radiokot.photoprism.api.albums.service

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbum
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import java.io.IOException

interface PhotoPrismAlbumsService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/albums")
    fun getAlbums(
        @Query("count")
        count: Int,
        @Query("offset")
        offset: Int,
        @Query("type")
        type: String,
        @Query("q")
        q: String? = null,
    ): List<PhotoPrismAlbum>
}
