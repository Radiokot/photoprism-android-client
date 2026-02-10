package ua.com.radiokot.photoprism.api.subjects.service

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import ua.com.radiokot.photoprism.api.subjects.model.PhotoPrismSubject
import java.io.IOException

interface PhotoPrismSubjectsService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/subjects")
    fun getSubjects(
        @Query("count")
        count: Int,
        @Query("offset")
        offset: Int,
        @Query("type")
        type: String,
        @Query("q")
        q: String? = null,
    ): List<PhotoPrismSubject>
}
