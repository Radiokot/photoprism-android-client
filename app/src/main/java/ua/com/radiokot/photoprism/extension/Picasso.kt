package ua.com.radiokot.photoprism.extension

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Target
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import kotlin.coroutines.cancellation.CancellationException

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

fun RequestCreator.intoSingle(): Single<Bitmap> = Single.create { emitter ->
    var isLoadingDone = false
    val target = object : Target {
        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
            if (!isLoadingDone) {
                isLoadingDone = true
                if (!emitter.isDisposed) {
                    emitter.onSuccess(bitmap)
                }
            }
        }

        override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
            if (!isLoadingDone) {
                isLoadingDone = true
                if (!emitter.isDisposed) {
                    emitter.tryOnError(e)
                }
            }
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            // Doesn't matter.
        }
    }

    // Emitter must keep a target reference
    // to save it from garbage collection during the loading,
    // because Picasso only keeps a weak reference.
    emitter.setCancellable {
        if (!isLoadingDone) {
            target.onBitmapFailed(CancellationException(), null)
        }
    }

    into(target)
}.subscribeOn(AndroidSchedulers.mainThread())
