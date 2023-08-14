package ua.com.radiokot.photoprism.features.gallery.view

import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchBookmarksBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.SearchBookmarkItem

class GallerySearchBookmarksView(
    private val view: ViewGallerySearchBookmarksBinding,
    private val viewModel: GallerySearchViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner, KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val log = kLogger("GallerySearchBookmarksView")

    init {
        subscribeToData()
        subscribeToState()
    }

    private var isListInitialized = false
    fun initListOnce() = view.bookmarksChipsLayout.post {
        if (isListInitialized) {
            return@post
        }

        view.bookmarksChipsLayout.setOnDragListener(
            SearchBookmarkViewDragListener(viewModel)
        )

        isListInitialized = true
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

        val bookmarkChipClickListener = View.OnClickListener { chip ->
            viewModel.onBookmarkChipClicked(chip.tag as SearchBookmarkItem)
        }
        val bookmarkChipEditClickListener = View.OnClickListener { chip ->
            viewModel.onBookmarkChipEditClicked(chip.tag as SearchBookmarkItem)
        }
        val bookmarkChipLongClickListener = View.OnLongClickListener { chip ->
            if (viewModel.canMoveBookmarks) {
                SearchBookmarkViewDragListener.beginDrag(chip as Chip)
            }
            true
        }

        with(view.bookmarksChipsLayout) {
            viewModel.bookmarks.observe(this@GallerySearchBookmarksView) { bookmarks ->
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

        viewModel.isBookmarksSectionVisible
            .observe(this) { view.root.isVisible = it }
    }

    private fun subscribeToState() {

    }
}
