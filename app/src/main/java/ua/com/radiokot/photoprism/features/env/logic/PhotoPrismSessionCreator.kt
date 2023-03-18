package ua.com.radiokot.photoprism.features.env.logic

import retrofit2.HttpException
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionCredentials
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection
import ua.com.radiokot.photoprism.features.env.data.model.InvalidCredentialsException
import java.net.HttpURLConnection

typealias PhotoPrismSessionServiceFactory =
            (apiUrl: String) -> PhotoPrismSessionService

class PhotoPrismSessionCreator(
    private val sessionServiceFactory: PhotoPrismSessionServiceFactory,
) : SessionCreator {

    override fun createSession(
        apiUrl: String,
        credentials: EnvConnection.Auth.Credentials
    ): String {
        return try {
            sessionServiceFactory(apiUrl)
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