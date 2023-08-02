package ua.com.radiokot.photoprism.extension

import android.view.View
import ua.com.radiokot.photoprism.util.ThrottleOnClickListener

/**
 * Sets [ThrottleOnClickListener] which prevents double clicks
 * by applying throttle for given interval.
 *
 * @param throttleIntervalMs interval in milliseconds during which subsequent clicks are ignored
 * @param l the listener to be launched once
 *
 */
fun View.setThrottleOnClickListener(
    throttleIntervalMs: Long = 600,
    l: (v: View) -> Unit,
) = setOnClickListener(
    ThrottleOnClickListener(
        throttleIntervalMs = throttleIntervalMs,
        onSingleClick = l,
    )
)
