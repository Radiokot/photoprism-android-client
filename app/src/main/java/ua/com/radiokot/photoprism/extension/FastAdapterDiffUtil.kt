package ua.com.radiokot.photoprism.extension

import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.DiffCallback
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil

/**
 * A [set] which prevents unwanted jumps and animations.
 */
fun <A : ItemAdapter<Item>, Item : GenericItem> FastAdapterDiffUtil.setBetter(
    recyclerView: RecyclerView,
    adapter: A,
    items: List<Item>,
    callback: DiffCallback<Item>,
    detectMoves: Boolean,
) {
    if (adapter.adapterItemCount == 0 || items.isEmpty()) {
        // Do not use DiffUtil to replace an empty list,
        // as it causes scrolling to the bottom.
        // Do not use it to set an empty list either,
        // as it causes an unnecessary "delete" animation.
        adapter.setNewList(items)
    } else {
        // Saving the layout manager state apparently prevents
        // DiffUtil-induced weird scrolling when items are inserted
        // outside the viewable area.
        val savedRecyclerState = recyclerView.layoutManager?.onSaveInstanceState()

        set(
            adapter = adapter,
            items = items,
            callback = callback,
            detectMoves = detectMoves,
        )

        if (savedRecyclerState != null) {
            recyclerView.layoutManager?.onRestoreInstanceState(savedRecyclerState)
        }
    }
}
