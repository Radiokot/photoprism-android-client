package ua.com.radiokot.photoprism.extension

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Ensures that the item of the position is visible. Scrolls to it otherwise.
 *
 * @param itemGlobalPosition global position of the item
 * (including all the possible headers and footers).
 */
fun RecyclerView.ensureItemIsVisible(
    itemGlobalPosition: Int,
) {
    val log = kLogger("RecyclerViewExtension")

    val layoutManager = (layoutManager as? LinearLayoutManager)
        .checkNotNull {
            "The recycler must have a layout manager at this moment"
        }

    val firstVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
    val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()

    if (itemGlobalPosition !in firstVisibleItemPosition..lastVisibleItemPosition) {
        log.debug {
            "ensureItemIsVisible(): scrolling_to_make_visible:" +
                    "\nitemGlobalPosition=$itemGlobalPosition"
        }

        scrollToPosition(itemGlobalPosition)
    } else {
        log.debug {
            "ensureItemIsVisible(): item_is_already_visible:" +
                    "\nitemGlobalPosition=$itemGlobalPosition"
        }
    }

    // Move the focus to the corresponding item.
    // It only does this when another item was focused before,
    // e.g. when navigating using a keyboard.
    post {
        layoutManager.findViewByPosition(itemGlobalPosition)?.requestFocus()
    }
}
