package ua.com.radiokot.photoprism.features.gallery.search.view

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchConfigurationBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.bindCheckedTwoWay
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.GallerySearchAlbumsView
import ua.com.radiokot.photoprism.features.gallery.search.people.view.GallerySearchPeopleView
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel

class GallerySearchConfigurationView(
    private val view: ViewGallerySearchConfigurationBinding,
    private val viewModel: GallerySearchViewModel,
    private val activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner = activity,
) : LifecycleOwner by lifecycleOwner, KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

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
    private val colorOnSurfaceVariant: Int by lazy {
        MaterialColors.getColor(
            view.root,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
    }

    private var isInitialized = false
    fun initOnce() {
        if (isInitialized) {
            return
        }

        bookmarksView.initListOnce()
        peopleView.initListOnce()
        albumsView.initOnce()

        subscribeToData()

        isInitialized = true
    }

    private fun subscribeToData() {
        val context = view.mediaTypeChipsLayout.context

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

        // TODO: move the following to a separate view.
        val chipSpacing =
            context.resources.getDimensionPixelSize(R.dimen.gallery_search_chip_spacing)
        val chipContext = ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Widget_Material3_Chip_Filter
        )
        val chipLayoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            context.resources.getDimensionPixelSize(R.dimen.gallery_search_chip_height),
        ).apply {
            setMargins(0, 0, chipSpacing, chipSpacing)
        }
        val chipIconTint = ColorStateList.valueOf(colorOnSurfaceVariant)

        with(view.mediaTypeChipsLayout) {
            viewModel.availableMediaTypes.observe(this@GallerySearchConfigurationView) { availableTypes ->
                availableTypes.forEach { mediaTypeName ->
                    addView(
                        Chip(chipContext).apply {
                            tag = mediaTypeName
                            setText(
                                ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources.getName(
                                    mediaTypeName
                                )
                            )
                            setChipIconResource(
                                ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources.getIcon(
                                    mediaTypeName
                                )
                            )
                            setChipIconTint(chipIconTint)

                            setEnsureMinTouchTargetSize(false)
                            isCheckable = true

                            setOnClickListener {
                                viewModel.onAvailableMediaTypeClicked(mediaTypeName)
                            }
                        },
                        chipLayoutParams,
                    )
                }
            }

            viewModel.selectedMediaTypes.observe(this@GallerySearchConfigurationView) { selectedTypes ->
                forEach { chip ->
                    with(chip as Chip) {
                        isChecked = selectedTypes?.contains(tag) == true
                        isCheckedIconVisible = isChecked
                        isChipIconVisible = !isChecked
                    }
                }
            }
        }

        viewModel.areSomeTypesUnavailable.observe(
            this,
            view.typesNotAvailableNotice::isVisible::set
        )
    }
}
