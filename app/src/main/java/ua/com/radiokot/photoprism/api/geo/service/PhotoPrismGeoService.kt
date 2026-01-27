package ua.com.radiokot.photoprism.api.geo.service

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.io.IOException

interface PhotoPrismGeoService {

    /**
     * @return GeoJSON with Point for each photo
     * ```
     *     {
     *       "id": "9",
     *       "type": "Feature",
     *       "geometry": {
     *         "type": "Point",
     *         "coordinates": [-2.59085833333333, 42.5683033333333]
     *       },
     *       "properties": {
     *         "Hash": "4bc82c3ea5aaa323aea801fe0125b554af8e49af",
     *         "TakenAt": "2012-08-27T12:40:25Z",
     *         "Title": "Bodegas Ysios Winery / Laguardia / Spain",
     *         "UID": "pt986fden7x4i71c"
     *       }
     *     },
     * ```
     */
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
