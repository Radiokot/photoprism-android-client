package ua.com.radiokot.photoprism.env.logic

import retrofit2.HttpException
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionCredentials
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import java.net.HttpURLConnection

class PhotoPrismSessionCreator(
    private val sessionService: PhotoPrismSessionService,
    private val envConnectionParams: EnvConnectionParams,
) : SessionCreator {

    override fun createSession(
        auth: EnvAuth
    ): EnvSession {
        return try {
            when (auth) {
                is EnvAuth.Credentials ->
                    sessionService
                        .createSession(
                            PhotoPrismSessionCredentials(
                                username = auth.username,
                                password = auth.password,
                            )
                        )
                        .let { photoPrismSession ->
                            EnvSession(
                                photoPrismSession = photoPrismSession,
                                envConnectionParams = envConnectionParams,
                            )
                        }

                EnvAuth.Public ->
                    EnvSession.public(
                        envConnectionParams = envConnectionParams,
                    )
            }
        } catch (e: HttpException) {
            // Only throw the credentials exception for a genuine PhotoPrism response.
            // There may be 401s due to the HTTP auth or deployment issues.
            if (e.code() == HttpURLConnection.HTTP_UNAUTHORIZED
                && e.response()?.errorBody()?.contentType()?.subtype == "json"
            ) {
                throw InvalidCredentialsException()
            } else {
                throw e
            }
        }
    }
}
