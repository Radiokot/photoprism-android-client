package ua.com.radiokot.photoprism.api.session.service

import retrofit2.http.Body
import retrofit2.http.POST
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSession
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionCredentials
import java.io.IOException

interface PhotoPrismSessionService {
    @kotlin.jvm.Throws(IOException::class)
    @POST("v1/session")
    fun createSession(@Body credentials: PhotoPrismSessionCredentials): PhotoPrismSession
}