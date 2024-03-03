package ua.com.radiokot.photoprism.env.data.model

import androidx.core.util.Predicate
import com.fasterxml.jackson.core.JsonParseException
import retrofit2.HttpException
import ua.com.radiokot.photoprism.env.data.model.ProxyBlockingAccessException.Companion.THROWABLE_PREDICATE
import java.io.IOException
import java.net.HttpURLConnection

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
            // Expected JSON but got HTML – puter calls for hooman.
            (it is JsonParseException
                    && it.message?.contains("character ('<'") == true)

                    ||
                    // Authelia redirect by 401 for non-GET requests.
                    // https://github.com/authelia/authelia/blob/616fa3c48d03d2f91e106a692cd4e6f9c209ae81/docs/content/en/integration/proxies/introduction.md#response-statuses
                    (it is HttpException
                            && it.code() == HttpURLConnection.HTTP_UNAUTHORIZED
                            && it.response()?.headers()?.get("Location") != null)

        }
    }
}
