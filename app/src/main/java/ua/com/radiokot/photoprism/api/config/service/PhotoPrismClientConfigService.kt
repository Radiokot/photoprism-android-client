package ua.com.radiokot.photoprism.api.config.service

import retrofit2.http.GET
import ua.com.radiokot.photoprism.api.config.model.PhotoPrismClientConfig

interface PhotoPrismClientConfigService {
    @GET("v1/config")
    fun getClientConfig(): PhotoPrismClientConfig
}