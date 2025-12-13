package ua.com.radiokot.photoprism.features.gallery.view

import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigationrail.NavigationRailView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetector
import ua.com.radiokot.photoprism.features.gallery.search.view.GallerySearchBarView
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel

class GalleryNavigationView(
    private val viewModel: GalleryViewModel,
) : KoinComponent {
    private var closeDrawer: (() -> Unit)? = null
    val backPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                closeDrawer?.invoke()
            }
        }

    fun initWithDrawer(
        drawerLayout: DrawerLayout,
        navigationView: NavigationView,
        searchBarView: GallerySearchBarView,
    ) {
        initMenu(
            navigationMenu = navigationView.menu,
            onItemClicked = drawerLayout::close,
        )

        searchBarView.addNavigationMenuIcon {
            drawerLayout.openDrawer(navigationView, true)
        }

        // Close the drawer on back press
        // if a significant part of it is visible.
        closeDrawer = drawerLayout::closeDrawers
        drawerLayout.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
               backPressedCallback.isEnabled = slideOffset >= 0.4f
            }
        })
    }

    fun initWithRail(
        navigationRail: NavigationRailView,
    ) {
        val tvDetector: TvDetector by inject()
        
        // On TV, replace Library item with Search
        if (tvDetector.isRunningOnTv) {
            navigationRail.menu.findItem(R.id.all)?.apply {
                setIcon(R.drawable.ic_search)
                setTitle(R.string.search)
            }
        }
        
        initMenu(
            navigationMenu = navigationRail.menu,
            isOnTv = tvDetector.isRunningOnTv,
        )
    }

    private fun initMenu(
        navigationMenu: Menu,
        onItemClicked: () -> Unit = {},
        isOnTv: Boolean = false,
    ) {
        fun getClickListener(extraAction: () -> Any) = MenuItem.OnMenuItemClickListener {
            extraAction()
            onItemClicked()
            true
        }

        // On TV, the "all" item is replaced with search, otherwise it's the library view
        navigationMenu.findItem(R.id.all)
            ?.setOnMenuItemClickListener(
                if (isOnTv) {
                    getClickListener(viewModel::onSearchClicked)
                } else {
                    null // On non-TV, it's a checkable item, not clickable
                }
            )

        navigationMenu.findItem(R.id.albums)
            .setOnMenuItemClickListener(getClickListener(viewModel::onAlbumsClicked))

        navigationMenu.findItem(R.id.favorites)
            .setOnMenuItemClickListener(getClickListener(viewModel::onFavoritesClicked))

        navigationMenu.findItem(R.id.calendar)
            .setOnMenuItemClickListener(getClickListener(viewModel::onCalendarClicked))

        navigationMenu.findItem(R.id.labels)
            .setOnMenuItemClickListener(getClickListener(viewModel::onLabelsClicked))

        navigationMenu.findItem(R.id.folders)
            .setOnMenuItemClickListener(getClickListener(viewModel::onFoldersClicked))

        navigationMenu.findItem(R.id.preferences)
            .setOnMenuItemClickListener(getClickListener(viewModel::onPreferencesClicked))
    }
}
