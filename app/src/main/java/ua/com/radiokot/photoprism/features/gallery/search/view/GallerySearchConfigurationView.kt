package ua.com.radiokot.photoprism.features.gallery.search.view

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchConfigurationBinding
import ua.com.radiokot.photoprism.extension.bindCheckedTwoWay
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.GallerySearchAlbumsView
import ua.com.radiokot.photoprism.features.gallery.search.people.view.GallerySearchPeopleView
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel

/**
 * A view for configuring gallery search.
 */
class GallerySearchConfigurationView(
    private val view: ViewGallerySearchConfigurationBinding,
    private val viewModel: GallerySearchViewModel,
    private val activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner = activity,
) : LifecycleOwner by lifecycleOwner {

    private val bookmarksView = GallerySearchBookmarksView(
        view = view.bookmarksView,
        viewModel = viewModel,
        lifecycleOwner = this,
    )
    private val peopleView = GallerySearchPeopleView(
        view = view.peopleView,
        viewModel = viewModel.peopleViewModel,
        activity = activity,
        lifecycleOwner = this,
    )
    private val albumsView = GallerySearchAlbumsView(
        view = view.albumsView,
        viewModel = viewModel.albumsViewModel,
        activity = activity,
        lifecycleOwner = this,
    )
    private val mediaTypesView = GallerySearchMediaTypesView(
        view = view.mediaTypesView,
        viewModel = viewModel,
        lifecycleOwner = this,
    )

    private var isInitialized = false
    fun initOnce() {
        if (isInitialized) {
            return
        }

        bookmarksView.initOnce()
        peopleView.initOnce()
        albumsView.initOnce()
        mediaTypesView.initOnce()

        subscribeToData()

        isInitialized = true
    }

    private fun subscribeToData() {
        viewModel.isApplyButtonEnabled
            .observe(this, view.searchButton::setEnabled)

        view.privateContentSwitch.bindCheckedTwoWay(viewModel.includePrivateContent)
        view.onlyFavoriteSwitch.bindCheckedTwoWay(viewModel.onlyFavorite)

        view.searchButton.setThrottleOnClickListener {
            viewModel.onSearchClicked()
        }

        view.resetButton.setThrottleOnClickListener {
            viewModel.onResetClicked()
        }
    }
}
