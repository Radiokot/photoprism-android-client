package ua.com.radiokot.photoprism.util

import android.os.SystemClock
import android.view.View

/**
 * A [View.OnClickListener] which prevents double clicks
 * by applying throttle for given interval.
 *
 * @param throttleIntervalMs interval in milliseconds during which subsequent clicks are ignored
 * @param onSingleClick the listener to be launched once
 */
class ThrottleOnClickListener(
    private val throttleIntervalMs: Long = 600,
    private val onSingleClick: (v: View) -> Unit,
) : View.OnClickListener {

    private var lastClickTimeMs: Long = 0

    override fun onClick(v: View) {
        val currentClickTimeMs = SystemClock.elapsedRealtime()
        if (currentClickTimeMs - lastClickTimeMs <= throttleIntervalMs) {
            return
        }
        lastClickTimeMs = currentClickTimeMs
        onSingleClick(v)
    }
}
