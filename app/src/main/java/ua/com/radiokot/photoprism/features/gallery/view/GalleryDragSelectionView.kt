package ua.com.radiokot.photoprism.features.gallery.view

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import ua.com.radiokot.photoprism.extension.filterIsInstance
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListViewModel

class GalleryDragSelectionView(
    private val viewModel: GalleryListViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {

    fun init(
        globalListAdapter: FastAdapter<*>,
        recyclerView: RecyclerView,
    ) {
        val receiver = object : DragSelectTouchListener.Receiver {
            override fun setSelected(indexSelection: Sequence<Pair<Int, Boolean>>) {
                viewModel.onGalleryMediaItemsDragSelectionChanged(
                    itemSelection = indexSelection
                        .mapNotNull { (index, isSelected) ->
                            (globalListAdapter.getItem(index) as? GalleryListItem.Media)
                                ?.let { it to isSelected }
                        }
                )
            }

            override fun isSelected(index: Int): Boolean =
                (globalListAdapter.getItem(index) as? GalleryListItem.Media)
                    ?.isMediaSelected == true
        }

        val listener = DragSelectTouchListener.create(
            context = recyclerView.context,
            receiver = receiver,
        )

        viewModel.itemListEvents.observeOnMain()
            .filterIsInstance<GalleryListViewModel.Event.ActivateDragSelection>()
            .subscribe(this) { activationEvent ->
                // A workaround to prevent swipe refresh from intercepting the drag
                // when the list is at the top.
                if (!recyclerView.canScrollVertically(-1)) {
                    recyclerView.scrollBy(0, 1)
                }

                listener.setIsActive(
                    active = true,
                    initialSelection = activationEvent.startGlobalPosition,
                )
            }

        viewModel.itemListState.observeOnMain().subscribe(this) { state ->
            if (state is GalleryListViewModel.State.Viewing) {
                listener.setIsActive(
                    active = false,
                    initialSelection = -1,
                )
            }
        }

        recyclerView.addOnItemTouchListener(listener)
    }
}
