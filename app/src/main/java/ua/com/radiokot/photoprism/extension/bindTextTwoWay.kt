package ua.com.radiokot.photoprism.extension

import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner

/**
 * Binds the given [liveData] to the text value in both directions.
 * The entered text is set to the [liveData], as well as the text from the [liveData],
 * if differs, is shown in the view keeping the cursor position.
 *
 * The view must be attached to a lifecycle owner.
 */
fun EditText.bindTextTwoWay(
    // TODO: Go to strings.
    liveData: MutableLiveData<CharSequence?>
) {
    val lifecycleOwner = findViewTreeLifecycleOwner()
        .checkNotNull {
            "The view must be attached to a lifecycle owner"
        }

    this.doOnTextChanged { text, _, _, _ ->
        liveData.value = text
    }

    liveData.observe(lifecycleOwner) { newText ->
        if (this.text != newText) {
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