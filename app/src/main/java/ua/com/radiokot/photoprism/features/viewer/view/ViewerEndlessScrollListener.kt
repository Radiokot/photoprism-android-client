package ua.com.radiokot.photoprism.features.viewer.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.GenericItemAdapter
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import ua.com.radiokot.photoprism.extension.checkNotNull

class ViewerEndlessScrollListener(
    recyclerView: RecyclerView,
    isLoadingLiveData: LiveData<Boolean>,
    visibleThreshold: Int,
    onLoadMore: (currentPage: Int) -> Unit,
) : EndlessRecyclerOnScrollListener(
    layoutManager = recyclerView.layoutManager.checkNotNull {
        "There must be a layout manager at this point"
    },
    visibleThreshold = visibleThreshold,
    footerAdapter = GenericItemAdapter(),
) {
    private val _onLoadMore = onLoadMore

    init {
        val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner().checkNotNull {
            "The recycler must be attached to a lifecycle owner"
        }
        isLoadingLiveData.observe(lifecycleOwner) { isLoading ->
            if (isLoading) {
                disable()
            } else {
                enable()
            }
        }
    }

    override fun onLoadMore(currentPage: Int) {
        if (currentPage == 0) {
            // Filter out false-triggering.
            return
        }

        _onLoadMore(currentPage)
    }
}
