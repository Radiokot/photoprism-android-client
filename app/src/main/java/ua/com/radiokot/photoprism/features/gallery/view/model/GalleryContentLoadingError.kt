package ua.com.radiokot.photoprism.features.gallery.view.model

import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import ua.com.radiokot.photoprism.env.data.model.TfaRequiredException
import ua.com.radiokot.photoprism.extension.shortSummary
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface GalleryContentLoadingError {
    /**
     * Can't establish a connection with the library
     */
    object LibraryNotAccessible : GalleryContentLoadingError

    /**
     * Automatic session renewal failed because the credentials
     * have been changed. Disconnect is required.
     */
    object CredentialsHaveBeenChanged : GalleryContentLoadingError

    /**
     * The session is expired and can't be renewed automatically.
     * Disconnect is required.
     */
    object SessionHasBeenExpired : GalleryContentLoadingError

    /**
     * The data has been requested, but something went wrong
     * while receiving the response. Генерал Файлюра arrived.
     */
    class GeneralFailure(val shortSummary: String) : GalleryContentLoadingError

    companion object {
        fun from(error: Throwable): GalleryContentLoadingError = when (error) {
            is UnknownHostException,
            is NoRouteToHostException,
            is SocketTimeoutException ->
                LibraryNotAccessible

            is InvalidCredentialsException ->
                CredentialsHaveBeenChanged

            is TfaRequiredException ->
                SessionHasBeenExpired

            else ->
                GeneralFailure(
                    shortSummary = error.shortSummary
                )
        }
    }
}
