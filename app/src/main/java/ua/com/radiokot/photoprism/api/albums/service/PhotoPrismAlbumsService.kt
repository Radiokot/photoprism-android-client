package ua.com.radiokot.photoprism.api.albums.service

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbum
import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbumCreation
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismBatchPhotoUids
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

    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @POST("v1/albums")
    fun createAlbum(
        @Body
        creation: PhotoPrismAlbumCreation,
    ): PhotoPrismAlbum

    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @POST("v1/albums/{albumUid}/photos")
    fun addPhotos(
        @Path("albumUid")
        albumUid: String,
        @Body
        batchPhotoUids: PhotoPrismBatchPhotoUids,
    ): Any
}
