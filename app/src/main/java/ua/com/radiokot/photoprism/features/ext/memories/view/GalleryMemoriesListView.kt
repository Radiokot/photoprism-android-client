package ua.com.radiokot.photoprism.features.ext.memories.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.adapters.ItemAdapter
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.ext.memories.view.model.GalleryMemoriesListViewModel
import ua.com.radiokot.photoprism.features.ext.memories.view.model.MemoriesListListItem
import ua.com.radiokot.photoprism.features.ext.memories.view.model.MemoryTitle
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
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

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
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

            is GalleryMemoriesListViewModel.Event.OpenDeletingConfirmationDialog ->
                openDeletingConfirmationDialog()
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

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
                        isPageIndicatorEnabled = true,
                        staticSubtitle = staticSubtitle.getString(activity),
                    )
                )
        )
    }

    private fun openDeletingConfirmationDialog() {
        MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.memory_deleting_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.onDeletingConfirmed()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        val RECYCLER_VIEW_TYPE = R.id.memories_recycler_view
    }
}
