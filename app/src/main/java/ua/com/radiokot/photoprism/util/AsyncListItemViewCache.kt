package ua.com.radiokot.photoprism.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.mikepenz.fastadapter.IItemViewGenerator
import ua.com.radiokot.photoprism.extension.kLogger
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors

typealias ListItemViewFactory = (
    context: Context,
    parent: ViewGroup?
) -> View

/**
 * A cache for heavy ViewHolders views.
 * It asynchronously inflates some amount of views in advance,
 * so by the time the list data is loaded no inflation needed.
 *
 * @see populateCache
 * @see getView
 */
class AsyncListItemViewCache(
    private val factory: ListItemViewFactory,
) : IItemViewGenerator {
    private val log = kLogger("AsyncListItemViewCache")
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable).apply { name = "AsyncListItemViewCacheExecutor" }
    }
    private val cache = LinkedList<WeakReference<View>>()

    /**
     * Starts asynchronous inflation of views and puts them into the cache.
     */
    fun populateCache(
        context: Context,
        parent: ViewGroup?,
        count: Int,
    ) {
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
    ): View =
        try {
            if (parent != null)
                generateView(context, parent)
            else
                generateView(context)
        } catch (e: Exception) {
            log.error(e) {
                "createView(): failed_to_create_view"
            }
            throw e
        }

    override fun generateView(ctx: Context): View =
        factory(ctx, null)

    override fun generateView(ctx: Context, parent: ViewGroup): View =
        factory(ctx, parent)
}