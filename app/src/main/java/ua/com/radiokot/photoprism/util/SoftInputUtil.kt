package ua.com.radiokot.photoprism.util

import android.content.Context
import android.view.Window
import android.view.inputmethod.InputMethodManager

/**
 * Contains utilities for soft keyboard interaction
 */
object SoftInputUtil {
    /**
     * Shows a soft keyboard on the currently focused view of the given [window]
     */
    fun showSoftInput(window: Window) {
        val view = window.currentFocus
            ?: return

        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val wasAcceptingText = imm.isAcceptingText
        val hasChanged = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)

        if (!wasAcceptingText && !hasChanged) {
            // Works fine anyway.
            @Suppress("DEPRECATION")
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
        }
    }

    /**
     * Hides a soft keyboard on the given [window]
     */
    fun hideSoftInput(window: Window) {
        val view = window.currentFocus
            ?: window.decorView

        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}