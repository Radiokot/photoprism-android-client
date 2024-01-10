package ua.com.radiokot.photoprism.env.data.model

import androidx.core.util.Predicate
import com.fasterxml.jackson.core.JsonParseException
import ua.com.radiokot.photoprism.env.data.model.ProxyBlockingAccessException.Companion.THROWABLE_PREDICATE
import java.io.IOException

class InvalidCredentialsException : IOException()

class EnvIsNotPublicException : IOException()

class SessionExpiredException(sessionId: String) :
    IOException(
        "The session '${
            sessionId.substring(
                0,
                sessionId.length.coerceAtMost(5)
            )
        }...' has expired"
    )

/**
 * Unexpected HTML response presumably means that the proxy is blocking API access
 * and wants to show a page instead.
 *
 * @see THROWABLE_PREDICATE
 */
class ProxyBlockingAccessException :
    IOException(
        "The proxy in front of the library blocks access to the API"
    ) {
    companion object {
        val THROWABLE_PREDICATE = Predicate<Throwable> {
            it is JsonParseException && it.message?.contains("character ('<'") == true
        }
    }
}
