package ua.com.radiokot.photoprism.util.images

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

/**
 * A [Target] allowing loading an image into a [TextView] spannable content.
 *
 * @param textView [TextView] in which the spannable content is shown
 * @param wrappedDrawable drawable included to the spannable content
 */
class TextViewWrappedDrawableTarget(
    private val textView: TextView,
    private val wrappedDrawable: SimpleWrappedDrawable,
) : Target {
    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
        wrappedDrawable.wrapped = bitmap.toDrawable(textView.context.resources)
        postTextUpdate()
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        if (errorDrawable != null) {
            wrappedDrawable.wrapped = errorDrawable
            postTextUpdate()
        }
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        if (placeHolderDrawable != null) {
            wrappedDrawable.wrapped = placeHolderDrawable
            postTextUpdate()
        }
    }

    private fun postTextUpdate() = textView.post {
        textView.text = textView.text
    }
}
