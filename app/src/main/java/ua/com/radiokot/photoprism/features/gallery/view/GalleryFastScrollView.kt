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
import io.reactivex.rxjava3.kotlin.subscribeBy
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.Predicate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.UTC_MONTH_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.capitalized
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryFastScrollViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMonthScrollBubble
import java.text.DateFormat

class GalleryFastScrollView(
    private val viewModel: GalleryFastScrollViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner,
    KoinComponent {

    private val log = kLogger("GalleryFastScrollView")

    private lateinit var fastScrollRecyclerView: RecyclerView
    private lateinit var fastScroller: FastScroller
    private val bubbleUtcMonthYearDateFormat: DateFormat by inject(named(UTC_MONTH_YEAR_DATE_FORMAT))
    private val bubbleUtcMonthDateFormat: DateFormat by inject(named(UTC_MONTH_DATE_FORMAT))
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

                        when (e.action) {
                            MotionEvent.ACTION_MOVE ->
                                onDragging()

                            MotionEvent.ACTION_UP ->
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
                getCurrentBubble()?.let { bubble ->
                    if (bubble.withYear)
                        bubbleUtcMonthYearDateFormat.format(bubble.localDate).capitalized()
                    else
                        bubbleUtcMonthDateFormat.format(bubble.localDate).capitalized()
                }
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
        subscribeToViewEvents()
    }

    private fun subscribeToData() {
        viewModel.bubbles.observe(this) { bubbles ->
            this.bubbles = bubbles
            updateScrollRange()
            notifyFastScroller()
        }
    }

    private fun subscribeToViewEvents() {
        viewModel.events
            .subscribeBy { event ->
                log.debug {
                    "subscribeToViewEvents(): received_new_event:" +
                            "\nevent=$event"
                }

                when (event) {
                    is GalleryFastScrollViewModel.Event.Reset -> {
                        resetScroll()
                    }

                    else -> {
                        "subscribeToFastScroll(): skipped_new_event:" +
                                "\nevent=$event"

                        return@subscribeBy
                    }
                }

                log.debug {
                    "subscribeToFastScroll(): handled_new_event:" +
                            "\nevent=$event"
                }
            }
            .autoDispose(this)
    }

    private fun resetScroll() {
        log.debug {
            "resetScroll(): resetting"
        }

        fastScrollViewHelper.scrollTo(0)
    }

    private fun updateScrollRange() {
        scrollRange =
            if (bubbles.size > 1)
            // For the fast scroller to appear, the range
            // must be greater than the view height.
                (fastScrollRecyclerView.height * 3)
                    // To avoid doubles, the range must exceed the number of bubbles.
                    .coerceAtLeast(bubbles.size * 2)
            else
                0

        log.debug {
            "updateScrollRange(): updated_scroll_range:" +
                    "\nnew=$scrollRange"
        }
    }

    private fun getCurrentBubble(): GalleryMonthScrollBubble? {
        val offsetRange = scrollRange - fastScrollRecyclerView.height
        val scrollOffset = scrollOffset
        val bubbles = bubbles

        if (bubbles.isEmpty() || offsetRange <= 0) {
            return null
        }

        val bubbleHeight = offsetRange / bubbles.size

        if (bubbleHeight == 0) {
            // Shouldn't ever get here, but who knows...
            log.warn {
                "getCurrentBubble(): not_enough_range:" +
                        "\noffsetRange=$offsetRange," +
                        "\nbubblesCount=${bubbles.size}"
            }
            return null
        }

        val bubbleIndex = (scrollOffset / bubbleHeight)
            .coerceAtMost(bubbles.size - 1)
        return bubbles.getOrNull(bubbleIndex)
    }

    private fun onDragEnded() {
        val currentBubble = getCurrentBubble()
        if (currentBubble != null) {
            viewModel.onDragEnded(currentBubble)
        }
    }

    private fun onDragging() {
        val currentBubble = getCurrentBubble()
        if (currentBubble != null) {
            viewModel.onDragging(currentBubble)
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
