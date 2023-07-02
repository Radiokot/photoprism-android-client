package ua.com.radiokot.photoprism.api.config.service

import retrofit2.http.GET
import retrofit2.http.Headers
import ua.com.radiokot.photoprism.api.config.model.PhotoPrismClientConfig
import java.io.IOException

interface PhotoPrismClientConfigService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/config")
    fun getClientConfig(): PhotoPrismClientConfig
}
