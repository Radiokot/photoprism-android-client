package ua.com.radiokot.photoprism.features.viewer.view

import android.view.View
import androidx.annotation.CallSuper
import com.mikepenz.fastadapter.FastAdapter
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage

/**
 * @see onContentPresented
 */
abstract class MediaViewerPageViewHolder<Page : MediaViewerPage>(
    itemView: View
) : FastAdapter.ViewHolder<Page>(itemView) {
    private var onContentPresentedListener: (() -> Unit)? = null

    /**
     * Whether the content is presented (image loading finished, video playback ended).
     */
    private var isContentPresented = false

    /**
     * Invokes given action once the content is presented
     * (image loading finished, video playback ended).
     */
    fun doOnContentPresented(onContentPresented: () -> Unit) {
        if (isContentPresented && itemView.isAttachedToWindow) {
            onContentPresented()
        } else {
            onContentPresentedListener = onContentPresented
        }
    }

    @CallSuper
    override fun bindView(item: Page, payloads: List<Any>) {
        isContentPresented = false
    }

    @CallSuper
    override fun detachFromWindow(item: Page) {
        // Once the page is detached,
        // the existing presentation status is no longer relevant.
        isContentPresented = false
    }

    protected fun onContentPresented() {
        isContentPresented = true

        if (itemView.isAttachedToWindow) {
            onContentPresentedListener?.invoke()
            onContentPresentedListener = null
        }
    }
}
