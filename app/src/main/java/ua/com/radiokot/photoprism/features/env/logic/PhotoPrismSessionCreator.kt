package ua.com.radiokot.photoprism.features.env.logic

import retrofit2.HttpException
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionCredentials
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection
import ua.com.radiokot.photoprism.features.env.data.model.InvalidCredentialsException
import java.net.HttpURLConnection

class PhotoPrismSessionCreator(
    private val sessionService: PhotoPrismSessionService,
) : SessionCreator {

    override fun createSession(
        credentials: EnvConnection.Auth.Credentials
    ): String {
        return try {
            sessionService
                .createSession(
                    PhotoPrismSessionCredentials(
                        username = credentials.username,
                        password = credentials.password,
                    )
                )
                .id
        } catch (e: HttpException) {
            if (e.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw InvalidCredentialsException()
            } else {
                throw e
            }
        }
    }
}