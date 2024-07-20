package ua.com.radiokot.photoprism.features.gallery.search.view

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchConfigBinding
import ua.com.radiokot.photoprism.extension.bindCheckedTwoWay
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.GallerySearchConfigAlbumsView
import ua.com.radiokot.photoprism.features.gallery.search.people.view.GallerySearchConfigPeopleView
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel

/**
 * A view for configuring gallery search.
 */
class GallerySearchConfigView(
    private val view: ViewGallerySearchConfigBinding,
    private val viewModel: GallerySearchViewModel,
    private val activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner = activity,
) : LifecycleOwner by lifecycleOwner {

    private val bookmarksView = GallerySearchConfigBookmarksView(
        view = view.bookmarksView,
        viewModel = viewModel,
        lifecycleOwner = this,
    )
    private val peopleView = GallerySearchConfigPeopleView(
        view = view.peopleView,
        viewModel = viewModel.peopleViewModel,
        activity = activity,
        lifecycleOwner = this,
    )
    private val albumsView = GallerySearchConfigAlbumsView(
        view = view.albumsView,
        viewModel = viewModel.albumsViewModel,
        activity = activity,
        lifecycleOwner = this,
    )
    private val mediaTypesView = GallerySearchConfigMediaTypesView(
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
