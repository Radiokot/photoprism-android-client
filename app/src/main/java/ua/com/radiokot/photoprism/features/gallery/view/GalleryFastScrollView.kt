package ua.com.radiokot.photoprism.features.gallery.view

import android.graphics.Canvas
import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.Predicate
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryFastScrollViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMonthScrollBubble
import kotlin.math.roundToInt

class GalleryFastScrollView(
    private val viewModel: GalleryFastScrollViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {
    private val log = kLogger("GalleryFastScrollView")

    private lateinit var fastScrollRecyclerView: RecyclerView
    private lateinit var fastScroller: FastScroller
    private var notifyFastScroller = {}
    private var bubbles: List<GalleryMonthScrollBubble> = emptyList()
    private var fastScrollRange = 0
    private var fastScrollOffset = 0

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
                return fastScrollRange
            }

            override fun getScrollOffset(): Int {
                return fastScrollOffset
            }

            override fun scrollTo(offset: Int) {
                fastScrollOffset = offset
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
                    fastScrollOffset = 0
                    notifyFastScroller()
                }
                GalleryFastScrollViewModel.State.Loading -> {
                    fastScrollOffset = 0
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
        fastScrollRange = when (viewModel.state.value) {
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
        val range = fastScrollRange
        if (range == 0) {
            return null
        }

        val scrollFraction = 2.0 * fastScrollOffset / range
        val bubbleIndex = ((bubbles.size - 1) * scrollFraction).roundToInt()
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
}