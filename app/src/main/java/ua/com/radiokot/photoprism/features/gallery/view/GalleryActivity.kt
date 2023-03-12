package ua.com.radiokot.photoprism.features.gallery.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import org.koin.android.ext.android.getKoin
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ActivityGalleryBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryProgressListItem


class GalleryActivity : AppCompatActivity() {
    private val sessionScope: Scope =
        getKoin().getScope("session")

    private lateinit var view: ActivityGalleryBinding
    private val viewModel: GalleryViewModel by sessionScope.inject()
    private val log = kLogger("GalleryActivity")

    private val galleryItemAdapter = ItemAdapter<GalleryMediaListItem>()
    private val galleryProgressFooterAdapter = ItemAdapter<GalleryProgressListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(view.root)

        view.galleryRecyclerView.post(::initList)

        viewModel.isLoading
            .observe(this) { isLoading ->
                view.isLoadingTextView.text = isLoading.toString()
                if (!isLoading) {
                    galleryProgressFooterAdapter.clear()
                } else if (galleryProgressFooterAdapter.adapterItemCount == 0) {
                    galleryProgressFooterAdapter.add(GalleryProgressListItem())
                }
            }

        viewModel.itemsList
            .observe(this) {
                if (it != null) {
                    galleryItemAdapter.setNewList(it)
                }
            }
    }

    private fun initList() {
        val galleryAdapter = FastAdapter.with(
            listOf(
                galleryItemAdapter,
                galleryProgressFooterAdapter
            )
        ).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item, _ ->
                if (item is GalleryMediaListItem) {
                    log.debug {
                        "gallery_item_clicked:" +
                                "\nsource=${item.source}"
                    }
                }
                false
            }
        }

        with(view.galleryRecyclerView) {
            val minItemWidthPx =
                resources.getDimensionPixelSize(R.dimen.list_item_gallery_media_min_size)
            val rowWidth = measuredWidth
            val spanCount = (rowWidth / minItemWidthPx).coerceAtLeast(1)

            log.debug {
                "initList(): calculated_span_count:" +
                        "\nspanCount=$spanCount," +
                        "\nrowWidth=$rowWidth," +
                        "\nminItemWidthPx=$minItemWidthPx"
            }

            val gridLayoutManager = GridLayoutManager(context, spanCount).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int =
                        if (galleryAdapter.getItemViewType(position) == R.id.list_item_gallery_progress)
                            spanCount
                        else
                            1
                }
            }

            adapter = galleryAdapter
            layoutManager = gridLayoutManager

            val endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener(
                footerAdapter = galleryProgressFooterAdapter,
                layoutManager = gridLayoutManager,
                visibleThreshold = gridLayoutManager.spanCount * 5
            ) {
                override fun onLoadMore(currentPage: Int) {
                    log.debug {
                        "onLoadMore(): load_more:" +
                                "\npage=$currentPage"
                    }
                    viewModel.loadMore()
                }
            }
            viewModel.isLoading.observe(this@GalleryActivity) { isLoading ->
                if (isLoading) {
                    endlessRecyclerOnScrollListener.disable()
                } else {
                    endlessRecyclerOnScrollListener.enable()
                }
            }
            addOnScrollListener(endlessRecyclerOnScrollListener)
        }
    }
}