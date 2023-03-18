package ua.com.radiokot.photoprism.env.data.model

import java.io.IOException

class InvalidCredentialsException: IOException()

class SessionExpiredException(sessionId: String) :
    IOException("The session '${sessionId.substring(0..(sessionId.length.coerceAtMost(5)))}...' has expired")