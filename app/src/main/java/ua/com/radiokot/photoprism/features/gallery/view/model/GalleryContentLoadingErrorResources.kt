package ua.com.radiokot.photoprism.features.gallery.view.model

import android.content.Context
import ua.com.radiokot.photoprism.R

object GalleryContentLoadingErrorResources {
    fun getMessage(
        error: GalleryContentLoadingError,
        context: Context
    ): String = when (error) {
        GalleryContentLoadingError.CredentialsHaveBeenChanged ->
            context.getString(R.string.error_invalid_password)

        is GalleryContentLoadingError.GeneralFailure ->
            context.getString(
                R.string.template_error_failed_to_load_content,
                error.shortSummary,
            )

        GalleryContentLoadingError.LibraryNotAccessible ->
            context.getString(R.string.error_library_not_accessible_try_again)

        GalleryContentLoadingError.SessionHasBeenExpired ->
            context.getString(R.string.error_session_expired)
    }
}
