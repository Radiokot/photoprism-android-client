package ua.com.radiokot.photoprism.features.gallery.search.view

import android.content.ClipData
import android.graphics.Rect
import android.view.DragEvent
import android.view.View
import android.view.View.OnDragListener
import android.view.ViewGroup
import androidx.core.view.forEachIndexed
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.search.view.model.SearchBookmarkItem

class SearchBookmarkViewDragListener(
    val viewModel: GallerySearchViewModel,
) : OnDragListener {
    private val log = kLogger("SearchBookmarkViewDrag")

    /**
     * For each area there is a preceding view (null at the start)
     */
    private val rectsWithPrecedingViews = mutableListOf<Pair<Rect, View?>>()

    override fun onDrag(v: View, event: DragEvent): Boolean = with(v as ViewGroup) {
        if (event.localState !is View) {
            return@with false
        }

        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                rectsWithPrecedingViews.clear()

                val layoutLocation = IntArray(2)
                    .also { getLocationOnScreen(it) }

                forEachIndexed { i, view ->
                    val viewRelativeLocation = IntArray(2)
                        .also { location ->
                            view.getLocationOnScreen(location)
                            location[0] -= layoutLocation[0]
                            location[1] -= layoutLocation[1]
                        }

                    val marginLayoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                    val viewRelativeRect = Rect(
                        viewRelativeLocation[0],
                        viewRelativeLocation[1],
                        viewRelativeLocation[0] + view.width
                                + marginLayoutParams.rightMargin
                                + marginLayoutParams.leftMargin,
                        viewRelativeLocation[1] + view.height
                                + marginLayoutParams.topMargin
                                + marginLayoutParams.bottomMargin
                    )

                    // If dropped in this rect (left half), will be placed before the current view.
                    val beforeViewRect = Rect(
                        viewRelativeRect.left,
                        viewRelativeRect.top,
                        viewRelativeRect.left + viewRelativeRect.width() / 2,
                        viewRelativeRect.top + viewRelativeRect.height() / 2
                    )
                    // If dropped in this rect (right half), will be placed next to the current view.
                    val nextToViewRect =
                        Rect(
                            beforeViewRect.right,
                            beforeViewRect.top,
                            viewRelativeRect.right,
                            viewRelativeRect.bottom
                        )
                    // For the last view the next rect width fills the parent.
                    if (i == childCount - 1) {
                        nextToViewRect.right = width
                    }

                    rectsWithPrecedingViews.add(beforeViewRect to getChildAt(i - 1))
                    rectsWithPrecedingViews.add(nextToViewRect to view)
                }

                log.debug {
                    "onDrag(): marked_indexed_regions:" +
                            "\nsize=${rectsWithPrecedingViews.size}"
                }

                return@with true
            }

            DragEvent.ACTION_DROP -> {
                if (!viewModel.canMoveBookmarks) {
                    return@with false
                }

                val matchingRectWithPrecedingView: Pair<Rect, View?>? =
                    rectsWithPrecedingViews.find { (rect, _) ->
                        rect.contains(event.x.toInt(), event.y.toInt())
                    }

                if (matchingRectWithPrecedingView != null) {
                    val precedingView = matchingRectWithPrecedingView.second
                    val precedingViewIndex = indexOfChild(precedingView)
                    val movedView = event.localState as View
                    val movedViewIndex = indexOfChild(movedView)

                    // Only initiate movement if dropped to a new position.
                    if (precedingView != movedView
                        && precedingViewIndex != movedViewIndex - 1
                    ) {
                        log.debug {
                            "onDrag(): dropped_to_new_position:" +
                                    "\nprecedingViewIndex=$precedingViewIndex," +
                                    "\nmovedViewIndex=$movedViewIndex"
                        }

                        if (precedingViewIndex == movedViewIndex + 1
                            || precedingViewIndex == movedViewIndex - 2
                        ) {
                            // Special handling for swap.
                            viewModel.onBookmarkChipsSwapped(
                                first = movedView.tag as SearchBookmarkItem,
                                second =
                                if (precedingViewIndex < movedViewIndex)
                                    getChildAt(movedViewIndex - 1).tag as SearchBookmarkItem
                                else
                                    getChildAt(movedViewIndex + 1).tag as SearchBookmarkItem
                            )
                        } else {
                            viewModel.onBookmarkChipMoved(
                                item = movedView.tag as SearchBookmarkItem,
                                placedAfter = precedingView
                                    ?.tag as? SearchBookmarkItem
                            )
                        }
                    }

                    return@with true
                } else {
                    log.debug {
                        "onDrag(): dropped_but_unmatched"
                    }

                    return@with false
                }
            }
        }

        return@with false
    }

    companion object {
        fun beginDrag(bookmarkView: View) {
            val dragShadow = View.DragShadowBuilder(bookmarkView)
            @Suppress("DEPRECATION")
            bookmarkView.startDrag(
                // Setting the clip data allows dropping the bookmark to the query field!
                // Do not set null
                ClipData.newPlainText(
                    "",
                    (bookmarkView.tag as SearchBookmarkItem).dragAndDropContent
                ),
                dragShadow,
                bookmarkView,
                0,
            )
        }
    }
}
