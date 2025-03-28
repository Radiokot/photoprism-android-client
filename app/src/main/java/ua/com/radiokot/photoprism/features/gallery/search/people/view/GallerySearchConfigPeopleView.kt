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
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchConfigPeopleBinding
import ua.com.radiokot.photoprism.extension.ensureItemIsVisible
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPeopleViewModel
import ua.com.radiokot.photoprism.features.people.view.PeopleSelectionActivity
import ua.com.radiokot.photoprism.features.people.view.model.SelectablePersonListItem

class GallerySearchConfigPeopleView(
    private val view: ViewGallerySearchConfigPeopleBinding,
    private val viewModel: GallerySearchPeopleViewModel,
    activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner = activity,
) : LifecycleOwner by lifecycleOwner {

    private val log = kLogger("GallerySearchConfigPeopleView")

    private val adapter = ItemAdapter<SelectablePersonListItem>()
    private val peopleSelectionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onPeopleSelectionResult
    )

    private var isInitialized = false
    fun initOnce() = view.peopleRecyclerView.post {
        if (isInitialized) {
            return@post
        }

        val listAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: SelectablePersonListItem, _ ->
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

        subscribeToState()
        subscribeToEvents()

        isInitialized = true
    }

    private fun subscribeToState() {
        viewModel.state.subscribe(this) { state ->
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
        }

        viewModel.isViewVisible.subscribe(this, view.root::isVisible::set)
    }

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
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
    }

    private fun openPeopleSelection(selectedPersonIds: Set<String>) {
        log.debug {
            "openPeopleSelection(): opening_selection:" +
                    "\nselectedPeopleCount=${selectedPersonIds.size}"
        }

        peopleSelectionLauncher.launch(
            Intent(view.root.context, PeopleSelectionActivity::class.java)
                .putExtras(
                    PeopleSelectionActivity.getBundle(
                        selectedPersonIds = selectedPersonIds,
                    )
                )
        )
    }

    private fun onPeopleSelectionResult(result: ActivityResult) {
        val bundle = result.data?.extras
        if (result.resultCode == Activity.RESULT_OK && bundle != null) {
            viewModel.onPeopleSelectionResult(
                newSelectedPersonIds = PeopleSelectionActivity.getSelectedPersonIds(
                    bundle
                )
            )
        }
    }
}
