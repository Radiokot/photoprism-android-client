package ua.com.radiokot.photoprism.extension

/**
 * @return given [block] result or null if an exception was occurred
 */
inline fun <R : Any> tryOrNull(block: () -> R?) = try {
    block()
} catch (_: Exception) {
    null
}
