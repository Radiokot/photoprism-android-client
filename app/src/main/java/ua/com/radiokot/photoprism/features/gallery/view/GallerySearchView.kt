package ua.com.radiokot.photoprism.features.gallery.view

import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.toSpannable
import androidx.core.view.forEach
import androidx.lifecycle.LifecycleOwner
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewGallerySearchContentBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.AppliedGallerySearch
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySearchViewModel
import kotlin.math.roundToInt


class GallerySearchView(
    private val viewModel: GallerySearchViewModel,
    lifecycleOwner: LifecycleOwner,
) : LifecycleOwner by lifecycleOwner {
    private val log = kLogger("GallerySearchView")

    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var configurationView: ViewGallerySearchContentBinding

    val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            searchView.hide()
        }
    }

    fun init(
        searchBar: SearchBar,
        searchView: SearchView,
        configurationView: ViewGallerySearchContentBinding,
    ) {
        this.searchBar = searchBar
        this.searchView = searchView
        this.configurationView = configurationView

        initView()

        subscribeToData()
        subscribeToState()
    }

    private fun initView() {
        var searchTextStash: Editable?
        searchView.addTransitionListener { _, previousState, newState ->
            log.debug {
                "initView(): search_view_transitioning:" +
                        "\nprevState=$previousState," +
                        "\nnewState=$newState"
            }

            when (newState) {
                SearchView.TransitionState.SHOWING -> {
                    viewModel.onConfigurationViewOpening()
                    searchTextStash = searchView.editText.text

                    // Override logic of the SearchView setting the SearchBar text.
                    searchView.editText.post {
                        searchView.editText.text = searchTextStash
                        searchView.editText.setSelection(searchTextStash?.length ?: 0)
                    }
                }
                SearchView.TransitionState.HIDING -> {
                    viewModel.onConfigurationViewClosing()
                }
                else -> {}
            }

            backPressedCallback.isEnabled = newState in setOf(
                SearchView.TransitionState.SHOWING,
                SearchView.TransitionState.SHOWN
            )
        }

        with(searchView.editText) {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    log.debug {
                        "initView(): edit_text_search_key_pressed"
                    }

                    viewModel.onSearchClicked()
                }

                false
            }

            bindTextTwoWay(viewModel.userQuery)
        }

        configurationView.searchButton.setOnClickListener {
            viewModel.onSearchClicked()
        }

        configurationView.resetButton.setOnClickListener {
            viewModel.onResetClicked()
        }

        searchBar.textView.ellipsize = TextUtils.TruncateAt.END
    }

    private fun subscribeToData() {
        val context = configurationView.mediaTypeChipsLayout.context
        viewModel.isApplyButtonEnabled
            .observe(this, configurationView.searchButton::setEnabled)

        val searchChipSpacing =
            context.resources.getDimensionPixelSize(R.dimen.gallery_search_media_type_chip_spacing)
        val searchChipContext = ContextThemeWrapper(
            context,
            R.style.MediaTypeChip
        )
        val searchChipLayoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            setMargins(0, 0, searchChipSpacing, searchChipSpacing)
        }

        with(configurationView.mediaTypeChipsLayout) {
            viewModel.availableMediaTypes.observe(this@GallerySearchView) { availableTypes ->
                availableTypes.forEach { mediaTypeName ->
                    addView(
                        Chip(searchChipContext).apply {
                            tag = mediaTypeName
                            setText(
                                GalleryMediaTypeResources.getName(
                                    mediaTypeName
                                )
                            )
                            setChipIconResource(
                                GalleryMediaTypeResources.getIcon(
                                    mediaTypeName
                                )
                            )
                            setEnsureMinTouchTargetSize(false)
                            isCheckable = true

                            setOnClickListener {
                                viewModel.onAvailableMediaTypeClicked(mediaTypeName)
                            }
                        },
                        searchChipLayoutParams,
                    )
                }
            }

            viewModel.selectedMediaTypes.observe(this@GallerySearchView) { selectedTypes ->
                forEach { chip ->
                    with(chip as Chip) {
                        if (selectedTypes.contains(tag)) {
                            isChecked = true
                            isCheckedIconVisible = true
                        } else {
                            isChecked = false
                            isCheckedIconVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToState() {
        viewModel.state.subscribe { state ->
            log.debug {
                "subscribeToState(): received_new_state:" +
                        "\nstate=$state"
            }

            // Override logic of the SearchBar text.
            searchBar.post {
                searchBar.text =
                    when (state) {
                        is GallerySearchViewModel.State.AppliedSearch ->
                            getSearchBarText(
                                search = state.search,
                                textView = searchBar.textView,
                            )
                        is GallerySearchViewModel.State.ConfiguringSearch ->
                            if (state.alreadyAppliedSearch != null)
                                getSearchBarText(
                                    search = state.alreadyAppliedSearch,
                                    textView = searchBar.textView,
                                )
                            else
                                null
                        GallerySearchViewModel.State.NoSearch ->
                            null
                    }

                when (state) {
                    is GallerySearchViewModel.State.AppliedSearch ->
                        closeConfigurationView()

                    is GallerySearchViewModel.State.ConfiguringSearch ->
                        openConfigurationView()

                    GallerySearchViewModel.State.NoSearch ->
                        closeConfigurationView()
                }

                log.debug {
                    "subscribeToState(): handled_new_state:" +
                            "\nstate=$state"
                }
            }
        }.disposeOnDestroy(this)
    }

    private fun closeConfigurationView() {
        searchView.hide()
    }

    private fun openConfigurationView() {
        searchView.show()
    }

    private fun getSearchBarText(
        search: AppliedGallerySearch,
        textView: TextView
    ): CharSequence {
        val iconPlaceholder = "* "
        val spannableString = SpannableStringBuilder()
            .apply {
                repeat(search.mediaTypes.size) {
                    append(iconPlaceholder)
                }

                if (search.mediaTypes.isNotEmpty()) {
                    append("  ")
                }
            }
            .append(search.userQuery)
            .toSpannable()

        val iconSize = (textView.lineHeight * 0.7).roundToInt()
        val textColors = textView.textColors

        search.mediaTypes.forEachIndexed { i, mediaType ->
            val drawable = ContextCompat.getDrawable(
                textView.context,
                GalleryMediaTypeResources.getIcon(mediaType)
            )!!.apply {
                setBounds(0, 0, iconSize, iconSize)
            }
            DrawableCompat.setTintList(drawable, textColors)

            spannableString.setSpan(
                ImageSpan(drawable, ImageSpan.ALIGN_BASELINE),
                i * iconPlaceholder.length,
                (i * iconPlaceholder.length) + 1,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        return spannableString
    }
}