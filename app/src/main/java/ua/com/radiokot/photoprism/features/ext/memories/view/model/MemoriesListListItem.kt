package ua.com.radiokot.photoprism.features.ext.memories.view.model

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.R

class MemoriesListListItem(
    private val viewModel: GalleryMemoriesListViewModel,
    private val lifecycleOwner: LifecycleOwner,
) : AbstractItem<MemoriesListListItem.ViewHolder>() {
    override val layoutRes: Int =
        R.layout.list_item_memories_list

    override val type: Int =
        R.id.memories_recycler_view

    override var identifier: Long =
        "memories".hashCode().toLong()

    override fun getViewHolder(v: View) = ViewHolder(
        viewModel = viewModel,
        lifecycleOwner = lifecycleOwner,
        itemView = v,
    )

    class ViewHolder(
        private val viewModel: GalleryMemoriesListViewModel,
        lifecycleOwner: LifecycleOwner,
        itemView: View
    ) : FastAdapter.ViewHolder<MemoriesListListItem>(itemView),
        LifecycleOwner by lifecycleOwner {

        private val memoriesRecyclerView = itemView as RecyclerView
        private val adapter = ItemAdapter<MemoryListItem>()
        private val itemsListObserver = Observer<List<MemoryListItem>> { memoryListItems ->
            memoryListItems?.let(adapter::setNewList)
        }

        init {
            memoriesRecyclerView.adapter = FastAdapter.with(adapter).apply {
                onClickListener = { _, _, item: MemoryListItem, _ ->
                    viewModel.onMemoryItemClicked(item)
                    true
                }
                onLongClickListener = {_, _, item: MemoryListItem, _ ->
                    viewModel.onMemoryItemLongClicked(item)
                    true
                }
            }
        }

        override fun bindView(item: MemoriesListListItem, payloads: List<Any>) {
            viewModel.itemsList.observe(this, itemsListObserver)
        }

        override fun unbindView(item: MemoriesListListItem) {
            viewModel.itemsList.removeObserver(itemsListObserver)
        }
    }
}
