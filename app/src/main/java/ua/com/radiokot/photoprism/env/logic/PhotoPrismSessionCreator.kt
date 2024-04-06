package ua.com.radiokot.photoprism.env.logic

import okhttp3.ResponseBody
import retrofit2.HttpException
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionCredentials
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionError
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.env.data.model.TfaCodeInvalidException
import ua.com.radiokot.photoprism.env.data.model.TfaRequiredException
import ua.com.radiokot.photoprism.extension.tryOrNull
import java.net.HttpURLConnection

class PhotoPrismSessionCreator(
    private val sessionService: PhotoPrismSessionService,
    private val envConnectionParams: EnvConnectionParams,
    private val jsonObjectMapper: JsonObjectMapper,
) : SessionCreator {

    override fun createSession(
        auth: EnvAuth,
        tfaCode: String?,
    ): EnvSession {
        return try {
            when (auth) {
                is EnvAuth.Credentials ->
                    sessionService
                        .createSession(
                            PhotoPrismSessionCredentials(
                                username = auth.username,
                                password = auth.password,
                                code = tfaCode,
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
            // Only handle genuine PhotoPrism responses.
            // There may be 401s due to the HTTP auth or deployment issues.
            val errorCode = e.code()
            val errorBody: ResponseBody? = e.response()?.errorBody()
            if (errorCode == HttpURLConnection.HTTP_UNAUTHORIZED
                && errorBody?.contentType()?.subtype == "json"
            ) {
                val photoPrismSessionError: PhotoPrismSessionError? = tryOrNull {
                    jsonObjectMapper.readValue(
                        errorBody.byteStream(),
                        PhotoPrismSessionError::class.java
                    )
                }

                when (photoPrismSessionError?.code) {
                    PhotoPrismSessionError.CODE_PASSCODE_REQUIRED ->
                        throw TfaRequiredException()

                    PhotoPrismSessionError.CODE_INVALID_PASSCODE ->
                        throw TfaCodeInvalidException()

                    else ->
                        throw InvalidCredentialsException()
                }
            } else {
                throw e
            }
        }
    }
}
