package ua.com.radiokot.photoprism.features.gallery.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.Predicate
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryFastScrollViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMonthScrollBubble

class GalleryFastScrollView(
    private val viewModel: GalleryFastScrollViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {
    private val log = kLogger("GalleryFastScrollView")

    private lateinit var fastScrollRecyclerView: RecyclerView
    private lateinit var fastScroller: FastScroller
    private var notifyFastScroller = {}
    private var bubbles: List<GalleryMonthScrollBubble> = emptyList()
    private var scrollRange = 0
    private var scrollOffset = 0
    private val context: Context
        get() = fastScrollRecyclerView.context

    private val bubbleBackground: Drawable by lazy {
        MaterialShapeDrawable.createWithElevationOverlay(
            context,
            context.resources.getDimensionPixelSize(R.dimen.fast_scroll_bubble_elevation)
                .toFloat()
        )
            .apply {
                setCornerSize(
                    context.resources.getDimensionPixelSize(R.dimen.fast_scroll_bubble_corner_size)
                        .toFloat()
                )
            }
    }

    private val fastScrollViewHelper: FastScroller.ViewHelper by lazy {
        object : FastScroller.ViewHelper {
            override fun addOnPreDrawListener(onPreDraw: Runnable) {
                fastScrollRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun onDraw(
                        c: Canvas,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        onPreDraw.run()
                    }
                })
            }

            override fun addOnScrollChangedListener(onScrollChanged: Runnable) {
                notifyFastScroller = onScrollChanged::run
            }

            override fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent>) {
                fastScrollRecyclerView.addOnItemTouchListener(object :
                    RecyclerView.SimpleOnItemTouchListener() {
                    override fun onInterceptTouchEvent(
                        rv: RecyclerView,
                        e: MotionEvent
                    ): Boolean {
                        return onTouchEvent.test(e)
                    }

                    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                        onTouchEvent.test(e)

                        if (e.action == MotionEvent.ACTION_UP) {
                            onDragEnded()
                        }
                    }
                })
            }

            override fun getScrollRange(): Int {
                return this@GalleryFastScrollView.scrollRange
            }

            override fun getScrollOffset(): Int {
                return this@GalleryFastScrollView.scrollOffset
            }

            override fun scrollTo(offset: Int) {
                this@GalleryFastScrollView.scrollOffset = offset
                fastScrollRecyclerView.invalidateItemDecorations()
                notifyFastScroller()
            }

            override fun getPopupText(): CharSequence? =
                getCurrentBubble()?.name
        }
    }

    fun init(
        fastScrollRecyclerView: RecyclerView,
    ) {
        this.fastScrollRecyclerView = fastScrollRecyclerView
        fastScroller = FastScrollerBuilder(fastScrollRecyclerView)
            .setViewHelper(fastScrollViewHelper)
            .useMd2Style()
            .setTrackDrawable(ContextCompat.getDrawable(context, R.drawable.fast_scroll_track)!!)
            .setThumbDrawable(ContextCompat.getDrawable(context, R.drawable.fast_scroll_thumb)!!)
            .setPopupStyle(::setUpBubble)
            .build()

        subscribeToData()
        subscribeToState()
    }

    private fun subscribeToData() {
        viewModel.bubbles.observe(this) { bubbles ->
            this.bubbles = bubbles
            updateScrollRange()
            notifyFastScroller()
        }
    }

    private fun subscribeToState() {
        viewModel.state.subscribe { state ->
            log.debug {
                "subscribeToState(): received_new_state:" +
                        "\nstate=$state"
            }

            updateScrollRange()

            when (state) {
                GalleryFastScrollViewModel.State.Idle -> {
                    scrollOffset = 0
                    notifyFastScroller()
                }
                GalleryFastScrollViewModel.State.Loading -> {
                    scrollOffset = 0
                    notifyFastScroller()
                }
                is GalleryFastScrollViewModel.State.AtMonth -> {
                }
            }

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.disposeOnDestroy(this)
    }

    private fun updateScrollRange() {
        scrollRange = when (viewModel.state.value) {
            is GalleryFastScrollViewModel.State.AtMonth,
            GalleryFastScrollViewModel.State.Idle -> {
                if (bubbles.size > 1)
                // For the fast scroller to appear, the range
                // must be greater than the view height.
                    fastScrollRecyclerView.height * 2
                else
                    0
            }
            GalleryFastScrollViewModel.State.Loading -> 0
        }
    }

    private fun getCurrentBubble(): GalleryMonthScrollBubble? {
        val scrollRange = scrollRange
        val scrollOffset = scrollOffset
        val bubbles = bubbles

        if (scrollRange == 0 || bubbles.isEmpty()) {
            return null
        }

        val trueScrollPosition = scrollOffset * 2
        val bubbleHeight = scrollRange / bubbles.size
        val bubbleIndex = (trueScrollPosition / bubbleHeight)
            .coerceAtMost(bubbles.size - 1)
        return bubbles.getOrNull(bubbleIndex)
    }

    private fun onDragEnded() {
        val currentBubble = getCurrentBubble()

        log.debug {
            "onDragEnded(): drag_ended:" +
                    "\ncurrentBubble=$currentBubble"
        }

        if (currentBubble != null) {
            viewModel.onScrolledToMonth(currentBubble)
        }
    }

    private fun setUpBubble(textView: TextView) = with(textView) {
        (layoutParams as MarginLayoutParams).apply {
            marginEnd =
                context.resources.getDimensionPixelSize(R.dimen.fast_scroll_bubble_end_spacing)
        }
        TextViewCompat.setTextAppearance(
            this,
            com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall
        )
        setTextColor(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface
            )
        )
        val verticalPadding =
            context.resources.getDimensionPixelSize(R.dimen.fast_scroll_bubble_padding_vertical)
        val horizontalPadding =
            context.resources.getDimensionPixelSize(R.dimen.fast_scroll_bubble_padding_horizontal)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        background = bubbleBackground
    }
}