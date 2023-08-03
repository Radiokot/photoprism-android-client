package ua.com.radiokot.photoprism.extension

import android.graphics.Bitmap
import android.os.Build
import com.squareup.picasso.RequestCreator

/**
 * Sets [Bitmap.Config.HARDWARE] decoding config if it is available.
 *
 * **It only suitable for immutable images!**
 */
fun RequestCreator.hardwareConfigIfAvailable(): RequestCreator = apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        config(Bitmap.Config.HARDWARE)
    }
}
