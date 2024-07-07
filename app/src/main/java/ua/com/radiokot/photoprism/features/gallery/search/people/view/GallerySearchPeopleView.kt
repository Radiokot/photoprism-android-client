package ua.com.radiokot.photoprism.features.gallery.search.people.view

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
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchPeopleBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.ensureItemIsVisible
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPeopleViewModel
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPersonListItem

class GallerySearchPeopleView(
    private val view: ViewGallerySearchPeopleBinding,
    private val viewModel: GallerySearchPeopleViewModel,
    activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner = activity,
) : LifecycleOwner by lifecycleOwner, KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    private val log = kLogger("GallerySearchPeopleView")

    private val adapter = ItemAdapter<GallerySearchPersonListItem>()
    private val peopleSelectionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onPeopleSelectionResult
    )

    init {
        subscribeToState()
        subscribeToEvents()
    }

    private var isListInitialized = false
    fun initListOnce() = view.peopleRecyclerView.post {
        if (isListInitialized) {
            return@post
        }

        val listAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: GallerySearchPersonListItem, _ ->
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

        view.peopleTitleLayout.setOnClickListener {
            viewModel.onSeeAllClicked()
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
            .subscribe(view.root::isVisible::set)
            .autoDispose(this)
    }

    private fun subscribeToEvents() = viewModel.events.subscribe { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            is GallerySearchPeopleViewModel.Event.OpenPeopleSelectionForResult ->
                openPeopleSelection(event.selectedPersonIds)

            is GallerySearchPeopleViewModel.Event.EnsureListItemVisible ->
                view.peopleRecyclerView.post {
                    view.peopleRecyclerView.ensureItemIsVisible(
                        itemGlobalPosition = adapter.getGlobalPosition(event.listItemIndex)
                    )
                }
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }.autoDispose(this)

    private fun openPeopleSelection(selectedPersonIds: Set<String>) {
        log.debug {
            "openPeopleSelection(): opening_selection:" +
                    "\nselectedPeopleCount=${selectedPersonIds.size}"
        }

        peopleSelectionLauncher.launch(
            Intent(view.root.context, GallerySearchPeopleSelectionActivity::class.java)
                .putExtras(
                    GallerySearchPeopleSelectionActivity.getBundle(
                        selectedPersonIds = selectedPersonIds,
                    )
                )
        )
    }

    private fun onPeopleSelectionResult(result: ActivityResult) {
        val bundle = result.data?.extras
        if (result.resultCode == Activity.RESULT_OK && bundle != null) {
            viewModel.onPeopleSelectionResult(
                newSelectedPersonIds = GallerySearchPeopleSelectionActivity.getSelectedPersonIds(bundle)
            )
        }
    }
}
