package ua.com.radiokot.photoprism.api.labels.service

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import ua.com.radiokot.photoprism.api.labels.model.PhotoPrismLabel
import java.io.IOException

interface PhotoPrismLabelsService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/labels")
    fun getLabels(
        @Query("count")
        count: Int,
        @Query("offset")
        offset: Int,
        @Query("all")
        all: Boolean,
    ): List<PhotoPrismLabel>
}
