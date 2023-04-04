package ua.com.radiokot.photoprism.extension

import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner

/**
 * Binds the given [liveData] to the text value in both directions.
 * The entered text is set to the [liveData], if differs ([String.equals]),
 * as well as the text from the [liveData] is shown in the view keeping the cursor position.
 */
fun EditText.bindTextTwoWay(
    liveData: MutableLiveData<String>,
    lifecycleOwner: LifecycleOwner = findViewTreeLifecycleOwner()
        .checkNotNull {
            "The view must be attached to a lifecycle owner"
        }
) {
    this.doOnTextChanged { text, _, _, _ ->
        val textString = text?.toString() ?: ""
        if (liveData.value != textString) {
            liveData.value = textString
        }
    }

    liveData.observe(lifecycleOwner) { newText ->
        val textString = this.text?.toString() ?: ""
        if (textString != newText) {
            if (newText != null) {
                if (selectionEnd == (text?.length ?: 0)) {
                    setText(newText)
                    setSelection(newText.length)
                } else {
                    setTextKeepState(newText)
                }
            } else {
                text = null
            }
        }
    }
}

/**
 * Binds the given [liveData] to the [Checkable.isChecked] value in both directions.
 * The current state is set to the [liveData], if differs,
 * as well as the state from the [liveData] is applied to the view.
 *
 * The binding uses [CompoundButton.setOnCheckedChangeListener], do not reset the listener.
 */
fun CompoundButton.bindCheckedTwoWay(
    liveData: MutableLiveData<Boolean>,
    lifecycleOwner: LifecycleOwner = findViewTreeLifecycleOwner()
        .checkNotNull {
            "The view must be attached to a lifecycle owner"
        }
) {
    this.setOnCheckedChangeListener { _, isChecked ->
        if (liveData.value != isChecked) {
            liveData.value = isChecked
        }
    }

    liveData.observe(lifecycleOwner) { newIsChecked ->
        if (isChecked != newIsChecked) {
            isChecked = newIsChecked
        }
    }
}