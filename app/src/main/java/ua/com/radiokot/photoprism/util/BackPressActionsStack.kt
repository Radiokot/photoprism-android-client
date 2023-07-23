package ua.com.radiokot.photoprism.util

import androidx.activity.OnBackPressedCallback
import java.util.Stack

/**
 * A stack of actions to be invoked on back button press.
 * Add [onBackPressedCallback] to the activity dispatcher.
 */
class BackPressActionsStack {
    private val stack = Stack<() -> Unit>()

    /**
     * A callback for an activity, which is enabled
     * if there are actions in the stack.
     */
    val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            stack.pop().invoke()
            onStackChanged()
        }
    }

    /**
     * Pushes the [action] to the stack removing any existing duplicates.
     */
    fun pushUniqueAction(action: () -> Unit) {
        removeAction(action)
        stack.push(action)
        onStackChanged()
    }

    /**
     * Removes the [action] from the stack.
     */
    fun removeAction(action: () -> Unit) {
        stack.removeAll(setOf(action))
        onStackChanged()
    }

    private fun onStackChanged() {
        onBackPressedCallback.isEnabled = stack.isNotEmpty()
    }
}
