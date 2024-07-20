package ua.com.radiokot.photoprism.features.gallery.search.view

import android.content.res.ColorStateList
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchMediaTypesBinding
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel

class GallerySearchMediaTypesView(
    private val view: ViewGallerySearchMediaTypesBinding,
    private val viewModel: GallerySearchViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {
    private val colorOnSurfaceVariant: Int by lazy {
        MaterialColors.getColor(
            view.root,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
    }

    private var isInitialized = false
    fun initOnce() = view.mediaTypeChipsLayout.post {
        if (isInitialized) {
            return@post
        }

        subscribeToData()

        isInitialized = true
    }

    private fun subscribeToData() {
        val context = view.mediaTypeChipsLayout.context
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
            viewModel.availableMediaTypes.observe(this@GallerySearchMediaTypesView) { availableTypes ->
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

            viewModel.selectedMediaTypes.observe(this@GallerySearchMediaTypesView) { selectedTypes ->
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
