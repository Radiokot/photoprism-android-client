package ua.com.radiokot.photoprism.features.gallery.view

import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchPeopleBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySearchPeopleViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.PersonListItem

class GallerySearchPeopleView(
    private val view: ViewGallerySearchPeopleBinding,
    private val viewModel: GallerySearchPeopleViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner, KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val log = kLogger("GallerySearchPeopleView")

    private val adapter = ItemAdapter<PersonListItem>()

    init {
        subscribeToState()
    }

    private var isListInitialized = false
    fun initListOnce() = view.peopleRecyclerView.post {
        if (isListInitialized) {
            return@post
        }

        val listAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: PersonListItem, _ ->
                viewModel.onPersonItemClicked(item)
                true
            }
        }

        with(view.peopleRecyclerView) {
            adapter = listAdapter
            // Layout manager is set in XML.
        }

        view.reloadPeopleButton.setOnClickListener {
            viewModel.onReloadPeopleClicked()
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
                    is GallerySearchPeopleViewModel.State.Ready ->
                        state.people

                    else ->
                        emptyList()
                }
            )

            view.loadingPeopleTextView.isVisible =
                state is GallerySearchPeopleViewModel.State.Loading

            view.reloadPeopleButton.isVisible =
                state is GallerySearchPeopleViewModel.State.LoadingFailed

            view.noPeopleFoundTextView.isVisible =
                state is GallerySearchPeopleViewModel.State.Ready && state.people.isEmpty()

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
