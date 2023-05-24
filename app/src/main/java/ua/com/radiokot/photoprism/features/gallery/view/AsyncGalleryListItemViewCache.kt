package ua.com.radiokot.photoprism.features.gallery.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors

// TODO: Make reusable
/**
 * A cache for heavy ViewHolders views.
 * It asynchronously inflates some amount of views in advance,
 * so by the time the list data is loaded no inflation needed.
 *
 * @see populateCache
 * @see getView
 */
class AsyncGalleryListItemViewCache {
    private val log = kLogger("AsyncViewCache")
    private val executor = Executors.newFixedThreadPool(4) { runnable ->
        Thread(runnable).apply { name = "AsyncViewCacheInitThread" }
    }
    private val cache = LinkedList<WeakReference<View>>()

    /**
     * Starts asynchronous inflation of views and puts them into the cache.
     */
    fun populateCache(
        context: Context,
        parent: ViewGroup?,
    ) {
        val count = 30

        log.debug {
            "populateCache(): starting:" +
                    "\ncount=$count"
        }

        cache.clear()

        val startedAt = System.currentTimeMillis()
        repeat(count) { i ->
            executor.submit {
                cache.push(WeakReference(createView(context, parent)))
                if (i == count - 1) {
                    log.debug {
                        "populateCache(): done:" +
                                "\ncount=$count," +
                                "\nelapsedMs=${System.currentTimeMillis() - startedAt}"
                    }
                }
            }
        }
    }

    /**
     * @return view from a cache, or, in case of a miss, newly inflated view.
     */
    fun getView(
        context: Context,
        parent: ViewGroup?,
    ): View =
        cache.poll()?.get()
            ?: createView(context, parent)
                .also {
                    log.debug {
                        "getView(): cache_miss"
                    }
                }

    private fun createView(
        context: Context,
        parent: ViewGroup?,
    ): View {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.list_item_gallery_media, parent, false)
        view.tag = GalleryListItem.Media.ViewHolder.ViewAttributes(view)
        return view
    }
}