package ua.com.radiokot.photoprism.extension

import android.os.Handler
import android.os.Looper

/**
 * @return given [block] result or null if an exception was occurred
 */
inline fun <R : Any> tryOrNull(block: () -> R?) = try {
    block()
} catch (_: Exception) {
    null
}

private val mHandler = Handler(Looper.getMainLooper())

fun runOnUiThread(action: () -> Unit) {
    mHandler.post(action)
}
