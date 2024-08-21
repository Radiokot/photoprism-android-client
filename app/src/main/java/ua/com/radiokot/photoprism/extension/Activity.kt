package ua.com.radiokot.photoprism.extension

import android.app.Activity
import androidx.activity.result.ActivityResult

/**
 * Sets [result] as the result of this activity and finishes it,
 * if the [result] is OK.
 */
fun Activity.proxyOkResult(result: ActivityResult) {
    if (result.resultCode == Activity.RESULT_OK) {
        setResult(result.resultCode, result.data)
        finish()
    }
}
