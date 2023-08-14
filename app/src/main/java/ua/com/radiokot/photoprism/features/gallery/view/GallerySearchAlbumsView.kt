package ua.com.radiokot.photoprism.features.gallery.view

import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchAlbumsBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.AlbumListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySearchAlbumsViewModel

class GallerySearchAlbumsView(
    private val view: ViewGallerySearchAlbumsBinding,
    private val viewModel: GallerySearchAlbumsViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner, KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val log = kLogger("GallerySearchAlbumsView")

    private val adapter = ItemAdapter<AlbumListItem>()

    init {
        subscribeToState()
    }

    private var isListInitialized = false
    fun initListOnce() = view.albumsRecyclerView.post {
        if (isListInitialized) {
            return@post
        }

        val listAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: AlbumListItem, _ ->
                viewModel.onAlbumItemClicked(item)
                true
            }
        }

        with(view.albumsRecyclerView) {
            adapter = listAdapter
            // Layout manager is set in XML.
        }

        view.albumsRecyclerView.setOnClickListener {
            viewModel.onReloadAlbumsClicked()
        }

        isListInitialized = true
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
            .subscribeBy { view.root.isVisible = it }
            .autoDispose(this)
    }
}
