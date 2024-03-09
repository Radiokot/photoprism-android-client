package ua.com.radiokot.photoprism.features.ext.memories.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.mikepenz.fastadapter.adapters.ItemAdapter
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.ext.memories.view.model.GalleryMemoriesListViewModel
import ua.com.radiokot.photoprism.features.ext.memories.view.model.MemoriesListListItem
import ua.com.radiokot.photoprism.features.ext.memories.view.model.MemoryTitle
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity

class GalleryMemoriesListView(
    private val viewModel: GalleryMemoriesListViewModel,
    private val activity: AppCompatActivity,
) : LifecycleOwner by activity {
    private val log = kLogger("GalleryMemoriesListView")

    val recyclerAdapter = ItemAdapter<MemoriesListListItem>()
    private val visibleContent = listOf(
        MemoriesListListItem(
            viewModel = viewModel,
            lifecycleOwner = this,
        )
    )

    init {
        viewModel.initOnce()

        subscribeToData()
        subscribeToEvents()
    }

    private fun subscribeToData() {
        viewModel.isViewVisible.observe(this) { isListVisible ->
            recyclerAdapter.setNewList(
                if (isListVisible)
                    visibleContent
                else
                    emptyList()
            )
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            is GalleryMemoriesListViewModel.Event.OpenViewer ->
                openViewer(
                    repositoryParams = event.repositoryParams,
                    staticSubtitle = event.staticSubtitle,
                )
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }.autoDispose(this)

    private fun openViewer(
        repositoryParams: SimpleGalleryMediaRepository.Params,
        staticSubtitle: MemoryTitle,
    ) {
        activity.startActivity(
            Intent(activity, MediaViewerActivity::class.java)
                .putExtras(
                    MediaViewerActivity.getBundle(
                        mediaIndex = 0,
                        repositoryParams = repositoryParams,
                        areActionsEnabled = true,
                        staticSubtitle = staticSubtitle.getString(activity),
                    )
                )
        )
    }

    companion object {
        val RECYCLER_VIEW_TYPE = R.id.memories_recycler_view
    }
}
