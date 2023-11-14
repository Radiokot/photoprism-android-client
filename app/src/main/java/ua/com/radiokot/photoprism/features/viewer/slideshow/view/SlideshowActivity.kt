package ua.com.radiokot.photoprism.features.viewer.slideshow.view

import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.GenericItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.EventHook
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivitySlideshowBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.recyclerView
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.viewer.slideshow.view.model.SlideshowViewModel
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerPageViewHolder
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerViewHolder
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoPlayerCacheViewModel
import kotlin.math.roundToInt

class SlideshowActivity : BaseActivity() {
    private val log = kLogger("SlideshowActivity")

    private lateinit var view: ActivitySlideshowBinding
    private val viewModel: SlideshowViewModel by viewModel()
    private val videoPlayerCacheViewModel: VideoPlayerCacheViewModel by viewModel()

    private val viewerPagesAdapter = ItemAdapter<MediaViewerPage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        view = ActivitySlideshowBinding.inflate(layoutInflater)
        setContentView(view.root)

        supportActionBar?.hide()

        val mediaIndex = intent.getIntExtra(MEDIA_INDEX_KEY, -1)
            .takeIf { it >= 0 }
            .checkNotNull {
                "Missing media index"
            }

        @Suppress("DEPRECATION")
        val repositoryParams: SimpleGalleryMediaRepository.Params =
            requireNotNull(intent.getParcelableExtra(REPO_PARAMS_KEY)) {
                "No repository params specified"
            }

        log.debug {
            "onCreate(): creating:" +
                    "\nmediaIndex=$mediaIndex," +
                    "\nrepositoryParams=$repositoryParams," +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        viewModel.initOnce(
            startPageIndex = mediaIndex,
            repositoryParams = repositoryParams,
        )

        // Init before the subscription.
        initPager(savedInstanceState)

        subscribeToData()

        initFullscreen()
    }

    private fun initFullscreen() = with(WindowInsetsControllerCompat(window, window.decorView)) {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun initPager(
        savedInstanceState: Bundle?,
    ) = with(view.viewPager) {
        // In slideshow mode no user input is allowed.
        isUserInputEnabled = false

        // Reset the image view size until it is obtained.
        viewModel.imageViewSize = Size(0, 0)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            // Image view size is the window size multiplied by a zoom factor.
            viewModel.imageViewSize = Size(
                (window.decorView.width * 1.5).roundToInt(),
                (window.decorView.height * 1.5).roundToInt()
            )
        }

        val fastAdapter = FastAdapter.with(viewerPagesAdapter).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            addEventHook(object : EventHook<MediaViewerPage> {
                override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                    if (viewHolder is VideoPlayerViewHolder) {
                        setUpVideoViewer(viewHolder)
                    }

                    return null
                }
            })

            // Set the required index once, after the data is set.
            if (savedInstanceState == null) {
                registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onChanged() {
                        val requiredPageIndex = viewModel.currentPageIndex.value
                            ?: return
                        recyclerView.scrollToPosition(requiredPageIndex)
                        unregisterAdapterDataObserver(this)
                    }
                })
            }
        }

        // When a page settles, make it notify the view model
        // when the content is presented (image is shown, video is ended).
        registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    onPageSettled(currentItem)
                }
            }

            override fun onPageSelected(position: Int) {
                if (scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                    onPageSettled(position)
                }
            }

            private var lastSettledPage = -1
            private fun onPageSettled(position: Int) {
                if (lastSettledPage == position) {
                    return
                }

                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                if (viewHolder is MediaViewerPageViewHolder<*>) {
                    viewHolder.doOnContentPresented {
                        viewModel.onPageContentPresented(position)
                    }
                }

                lastSettledPage = position
            }
        })

        adapter = fastAdapter

        // TODO: refactor to eliminate duplication.
        val endlessScrollListener = object : EndlessRecyclerOnScrollListener(
            footerAdapter = GenericItemAdapter(),
            layoutManager = recyclerView.layoutManager.checkNotNull {
                "There must be a layout manager at this point"
            },
            visibleThreshold = 6
        ) {
            override fun onLoadMore(currentPage: Int) {
                if (currentPage == 0) {
                    // Filter out false-triggering.
                    return
                }

                log.debug {
                    "onLoadMore(): load_more:" +
                            "\npage=$currentPage"
                }
                viewModel.loadMore()
            }
        }
        viewModel.isLoading.observe(this@SlideshowActivity) { isLoading ->
            if (isLoading) {
                endlessScrollListener.disable()
            } else {
                endlessScrollListener.enable()
            }
        }
        recyclerView.addOnScrollListener(endlessScrollListener)

        // Fancier animation between pages.
        setPageTransformer(DepthPageTransformer())
    }

    // TODO: refactor to eliminate duplication.
    private fun setUpVideoViewer(viewHolder: VideoPlayerViewHolder) {
        viewHolder.playerCache = videoPlayerCacheViewModel
        viewHolder.bindPlayerToLifecycle(this@SlideshowActivity.lifecycle)

        val playerControlsView = viewHolder.playerControlsLayout

        if (playerControlsView != null) {
            playerControlsView.root.isVisible = false
        }

        viewHolder.setOnFatalPlaybackErrorListener(viewModel::onVideoPlayerFatalPlaybackError)
    }

    private fun subscribeToData() {
        viewModel.isLoading.observe(this) { isLoading ->
            log.debug {
                "subscribeToData(): loading_changed:" +
                        "\nis_loading=$isLoading"
            }

            if (isLoading) {
                view.progressIndicator.show()
            } else {
                view.progressIndicator.hide()
            }
        }

        viewModel.itemsList.observe(this) {
            if (it != null) {
                viewerPagesAdapter.setNewList(it)
            }
        }

        viewModel.currentPageIndex.observe(this) {
            view.viewPager.setCurrentItem(it, true)
        }
    }

    companion object {
        private const val MEDIA_INDEX_KEY = "media-index"
        private const val REPO_PARAMS_KEY = "repo-params"

        /**
         * @param mediaIndex index of the media to start from
         * @param repositoryParams params of the media repository to view
         */
        fun getBundle(
            mediaIndex: Int,
            repositoryParams: SimpleGalleryMediaRepository.Params,
        ) = Bundle().apply {
            putInt(MEDIA_INDEX_KEY, mediaIndex)
            putParcelable(REPO_PARAMS_KEY, repositoryParams)
        }
    }
}
