package ua.com.radiokot.photoprism.env.data.model

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

class ProxyBlockingAccessException :
    IOException(
        "The proxy in front of the library blocks access to the API"
    )
