package ua.com.radiokot.photoprism.features.gallery.view

import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel

class GalleryNavigationView(
    private val viewModel: GalleryViewModel,
) {
    fun init(
        drawerLayout: DrawerLayout,
        navigationView: NavigationView,
    ) {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.folders ->
                    viewModel.onFoldersClicked()

                R.id.preferences ->
                    viewModel.onPreferencesButtonClicked()
            }

            drawerLayout.close()

            false
        }
    }
}
