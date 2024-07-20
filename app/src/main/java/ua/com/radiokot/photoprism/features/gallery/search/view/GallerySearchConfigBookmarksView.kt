package ua.com.radiokot.photoprism.features.gallery.search.view

import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchBookmarksBinding
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.search.view.model.SearchBookmarkItem
import ua.com.radiokot.photoprism.util.ThrottleOnClickListener

class GallerySearchConfigBookmarksView(
    private val view: ViewGallerySearchBookmarksBinding,
    private val viewModel: GallerySearchViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {

    private var isInitialized = false
    fun initOnce() = view.bookmarksChipsLayout.post {
        if (isInitialized) {
            return@post
        }

        view.bookmarksChipsLayout.setOnDragListener(
            SearchBookmarkViewDragListener(viewModel)
        )

        subscribeToData()

        isInitialized = true
    }

    private fun subscribeToData() {
        val context = view.bookmarksChipsLayout.context
        val chipSpacing =
            context.resources.getDimensionPixelSize(R.dimen.gallery_search_chip_spacing)
        val chipContext = ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Widget_Material3_Chip_Filter
        )
        val chipLayoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            context.resources.getDimensionPixelSize(R.dimen.gallery_search_chip_height),
        ).apply {
            setMargins(0, 0, chipSpacing, chipSpacing)
        }

        val bookmarkChipClickListener = ThrottleOnClickListener { chip ->
            viewModel.onBookmarkChipClicked(chip.tag as SearchBookmarkItem)
        }
        val bookmarkChipEditClickListener = ThrottleOnClickListener { chip ->
            viewModel.onBookmarkChipEditClicked(chip.tag as SearchBookmarkItem)
        }
        val bookmarkChipLongClickListener = View.OnLongClickListener { chip ->
            if (viewModel.canMoveBookmarks) {
                SearchBookmarkViewDragListener.beginDrag(chip as Chip)
            }
            true
        }

        with(view.bookmarksChipsLayout) {
            viewModel.bookmarks.observe(this@GallerySearchConfigBookmarksView) { bookmarks ->
                removeAllViews()
                bookmarks.forEach { bookmark ->
                    addView(Chip(chipContext).apply {
                        tag = bookmark
                        text = bookmark.name
                        setEnsureMinTouchTargetSize(false)
                        setOnClickListener(bookmarkChipClickListener)

                        isCheckable = false

                        setCloseIconResource(R.drawable.ic_pencil)
                        isCloseIconVisible = true
                        setOnCloseIconClickListener(bookmarkChipEditClickListener)

                        setOnLongClickListener(bookmarkChipLongClickListener)
                    }, chipLayoutParams)
                }
            }
        }

        viewModel.isBookmarksSectionVisible.observe(this, view.root::isVisible::set)
    }
}
