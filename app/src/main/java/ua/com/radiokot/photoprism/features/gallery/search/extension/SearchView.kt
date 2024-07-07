package ua.com.radiokot.photoprism.features.gallery.search.extension

import android.content.res.ColorStateList
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.color.MaterialColors
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModel

/**
 * Binds the raw input and the close/expand state.
 */
fun SearchView.bindToViewModel(
    viewModel: SearchViewViewModel,
    lifecycleOwner: LifecycleOwner = findViewTreeLifecycleOwner()
        .checkNotNull {
            "The view must be attached to a lifecycle owner"
        },
) {
    // Directly bind the input.
    findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        .bindTextTwoWay(viewModel.rawSearchInput, lifecycleOwner)

    // Bind the collapse/expand state.
    viewModel.isSearchExpanded.observe(lifecycleOwner) { isExpanded ->
        // This also triggers the following listeners.
        isIconified = !isExpanded
    }
    setOnSearchClickListener {
        viewModel.onSearchIconClicked()
    }
    setOnCloseListener {
        viewModel.onSearchCloseClicked()
        false
    }
}

/**
 * Applies the correct Material 3 icon tint to the close button.
 */
fun SearchView.fixCloseButtonColor() {
    with(findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)) {
        ImageViewCompat.setImageTintList(
            this, ColorStateList.valueOf(
                MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
        )
    }
}

fun SearchView.hideUnderline() {
    with(findViewById<View>(androidx.appcompat.R.id.search_plate)) {
        background = null
    }
}
