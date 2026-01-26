package ua.com.radiokot.photoprism.api.geo.service

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.io.IOException

interface PhotoPrismGeoService {

    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/geo")
    fun getGeoJson(
        @Query("count")
        count: Int,
        @Query("offset")
        offset: Int,
        @Query("public")
        public: Boolean = true,
        @Query("q")
        q: String? = null
    ): String
}
