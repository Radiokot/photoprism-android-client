package ua.com.radiokot.photoprism.features.gallery.view

import android.view.Menu
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigationrail.NavigationRailView
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.search.view.GallerySearchBarView
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel

class GalleryNavigationView(
    private val viewModel: GalleryViewModel,
) {
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
    }

    fun initWithRail(
        navigationRail: NavigationRailView,
    ) {
        initMenu(
            navigationMenu = navigationRail.menu,
        )
    }

    private fun initMenu(
        navigationMenu: Menu,
        onItemClicked: () -> Unit = {},
    ) {
        fun getClickListener(extraAction: () -> Any) = MenuItem.OnMenuItemClickListener {
            extraAction()
            onItemClicked()
            true
        }

        navigationMenu.findItem(R.id.albums)
            .setOnMenuItemClickListener(getClickListener(viewModel::onAlbumsClicked))

        navigationMenu.findItem(R.id.favorites)
            .setOnMenuItemClickListener(getClickListener(viewModel::onFavoritesClicked))

        navigationMenu.findItem(R.id.folders)
            .setOnMenuItemClickListener(getClickListener(viewModel::onFoldersClicked))

        navigationMenu.findItem(R.id.preferences)
            .setOnMenuItemClickListener(getClickListener(viewModel::onPreferencesClicked))
    }
}
