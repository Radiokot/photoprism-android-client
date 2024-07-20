package ua.com.radiokot.photoprism.features.gallery.search.albums.view

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import io.reactivex.rxjava3.kotlin.subscribeBy
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchAlbumsBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.ensureItemIsVisible
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumListItem
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumsViewModel

class GallerySearchAlbumsView(
    private val view: ViewGallerySearchAlbumsBinding,
    private val viewModel: GallerySearchAlbumsViewModel,
    activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner = activity,
) : LifecycleOwner by lifecycleOwner {

    private val log = kLogger("GallerySearchAlbumsView")

    private val adapter = ItemAdapter<GallerySearchAlbumListItem>()
    private val albumSelectionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onAlbumSelectionResult
    )

    private var isInitialized = false
    fun initOnce() = view.albumsRecyclerView.post {
        if (isInitialized) {
            return@post
        }

        val listAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: GallerySearchAlbumListItem, _ ->
                viewModel.onAlbumItemClicked(item)
                true
            }
        }

        with(view.albumsRecyclerView) {
            adapter = listAdapter
            // Layout manager is set in XML.
        }

        view.reloadAlbumsButton.setOnClickListener {
            viewModel.onReloadAlbumsClicked()
        }

        view.albumsTitleLayout.setOnClickListener {
            viewModel.onSeeAllClicked()
        }

        subscribeToState()
        subscribeToEvents()

        isInitialized = true
    }

    private fun subscribeToState() {
        viewModel.state.subscribeBy { state ->
            log.debug {
                "subscribeToState(): received_new_state:" +
                        "\nstate=$state"
            }

            adapter.setNewList(
                when (state) {
                    is GallerySearchAlbumsViewModel.State.Ready ->
                        state.albums

                    else ->
                        emptyList()
                }
            )

            view.loadingAlbumsTextView.isVisible =
                state is GallerySearchAlbumsViewModel.State.Loading

            view.reloadAlbumsButton.isVisible =
                state is GallerySearchAlbumsViewModel.State.LoadingFailed

            view.noAlbumsFoundTextView.isVisible =
                state is GallerySearchAlbumsViewModel.State.Ready && state.albums.isEmpty()

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.autoDispose(this)

        viewModel.isViewVisible
            .subscribe(view.root::isVisible::set)
            .autoDispose(this)
    }

    private fun subscribeToEvents() = viewModel.events.subscribe { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            is GallerySearchAlbumsViewModel.Event.OpenAlbumSelectionForResult ->
                openAlbumSelection(event.selectedAlbumUid)

            is GallerySearchAlbumsViewModel.Event.EnsureListItemVisible ->
                view.albumsRecyclerView.post {
                    view.albumsRecyclerView.ensureItemIsVisible(
                        itemGlobalPosition = adapter.getGlobalPosition(event.listItemIndex)
                    )
                }
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }.autoDispose(this)

    private fun openAlbumSelection(selectedAlbumUid: String?) {
        log.debug {
            "openAlbumSelection(): opening_selection:" +
                    "\nselectedAlbumUid=$selectedAlbumUid"
        }

        albumSelectionLauncher.launch(
            Intent(view.root.context, GallerySearchAlbumSelectionActivity::class.java)
                .putExtras(
                    GallerySearchAlbumSelectionActivity.getBundle(
                        selectedAlbumUid = selectedAlbumUid,
                    )
                )
        )
    }

    private fun onAlbumSelectionResult(result: ActivityResult) {
        val bundle = result.data?.extras
        if (result.resultCode == Activity.RESULT_OK && bundle != null) {
            viewModel.onAlbumSelectionResult(
                newSelectedAlbumUid = GallerySearchAlbumSelectionActivity.getSelectedAlbumUid(bundle)
            )
        }
    }
}
