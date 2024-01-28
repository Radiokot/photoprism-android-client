package ua.com.radiokot.photoprism.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IItemVHFactory
import ua.com.radiokot.photoprism.extension.kLogger
import java.util.concurrent.Executors

typealias ItemViewFactory = (ctx: Context, parent: ViewGroup?) -> View
typealias ItemViewHolderFactory<VH> = (itemView: View) -> VH

class AsyncRecycledViewPoolInitializer(
    private val fastAdapter: FastAdapter<*>,
    private val itemViewType: Int,
    private val itemViewFactory: ItemViewFactory,
    private val itemViewHolderFactory: ItemViewHolderFactory<ViewHolder>,
) {
    private val log = kLogger("AsyncRVPoolInitializer")
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable).apply { name = "AsyncRVPoolInitializerExecutor" }
    }

    /**
     * @param recycledViewsCount should match [RecyclerView.RecycledViewPool.setMaxRecycledViews]
     */
    fun initPool(
        recyclerView: RecyclerView,
        recycledViewsCount: Int,
    ) {
        log.debug {
            "initPool(): starting:" +
                    "\ncount=$recycledViewsCount"
        }

        fastAdapter.registerItemFactory(itemViewType, object : IItemVHFactory<ViewHolder> {
            override fun getViewHolder(parent: ViewGroup): ViewHolder =
                itemViewHolderFactory(itemViewFactory(parent.context, parent))
        })

        val startedAt = System.currentTimeMillis()
        repeat(recycledViewsCount) { i ->
            executor.submit {
                val viewHolder = fastAdapter.createViewHolder(recyclerView, itemViewType)
                recyclerView.recycledViewPool.putRecycledView(viewHolder)

                if (i == recycledViewsCount - 1) {
                    log.debug {
                        "initPool(): done:" +
                                "\ncount=$recycledViewsCount," +
                                "\nelapsedMs=${System.currentTimeMillis() - startedAt}"
                    }
                }
            }
        }
    }
}
