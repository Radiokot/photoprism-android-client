package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.google.android.material.search.SearchView
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityPhotoFrameWidgetConfigurationBinding
import ua.com.radiokot.photoprism.databinding.IncludePhotoFrameWidgetConfigurationCardContentBinding
import ua.com.radiokot.photoprism.databinding.IncludePhotoFrameWidgetConfigurationShapesBinding
import ua.com.radiokot.photoprism.extension.animateScale
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.bindCheckedTwoWay
import ua.com.radiokot.photoprism.extension.fadeIn
import ua.com.radiokot.photoprism.extension.fadeOut
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.gallery.search.view.AppliedGallerySearchSummaryFactory
import ua.com.radiokot.photoprism.features.gallery.search.view.GallerySearchView
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.model.PhotoFrameWidgetConfigurationViewModel
import kotlin.concurrent.thread

class PhotoFrameWidgetConfigurationActivity : BaseActivity() {
    private val log = kLogger("PhotoFrameWidgetConfigurationActivity")

    private lateinit var view: ActivityPhotoFrameWidgetConfigurationBinding
    private lateinit var cardContentView: IncludePhotoFrameWidgetConfigurationCardContentBinding
    private val viewModel: PhotoFrameWidgetConfigurationViewModel by viewModel()
    private val picasso: Picasso by inject()
    private val shapeScaleAnimationDuration: Int by lazy {
        resources.getInteger(android.R.integer.config_shortAnimTime) / 2
    }
    private val defaultShapeScale = 0.95f
    private val selectedShapeScale = 1.03f
    private val appWidgetId: Int by lazy {
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }
    private val searchView: GallerySearchView by lazy {
        GallerySearchView(
            viewModel = viewModel.searchViewModel,
            fragmentManager = supportFragmentManager,
            activity = this,
        )
    }
    private val searchSummaryFactory: AppliedGallerySearchSummaryFactory by lazy {
        AppliedGallerySearchSummaryFactory(
            picasso = picasso,
            viewModel = viewModel.searchViewModel,
        )
    }
    override val windowBackgroundColor: Int
        get() = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the widget host to cancel out of the widget placement
        // if the user presses the back button.
        setResult(RESULT_CANCELED)

        if (goToEnvConnectionIfNoSession() || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        view = ActivityPhotoFrameWidgetConfigurationBinding.inflate(layoutInflater)
        cardContentView =
            IncludePhotoFrameWidgetConfigurationCardContentBinding.bind(view.mainCardView)
        setContentView(view.root)

        viewModel.initOnce(
            widgetId = appWidgetId,
        )

        initShapesAsync()
        initSearch()
        initDate()
        initButtons()

        subscribeToEvents()

        onBackPressedDispatcher.addCallback(viewModel.backPressedCallback)
    }

    private fun initShapesAsync() = thread(name = "InitShapesThread") {
        val compositeDisposable = CompositeDisposable()

        val shapesLayout = IncludePhotoFrameWidgetConfigurationShapesBinding.inflate(
            layoutInflater,
            cardContentView.contentLayout,
            false
        ).root

        shapesLayout.forEach { view ->
            val viewShape = view.tag
                ?.takeIf { it is String }
                ?.runCatching { PhotoFrameWidgetShape.valueOf(toString()) }
                ?.getOrNull()
                ?: return@forEach

            view.setOnClickListener {
                viewModel.onShapeClicked(viewShape)
            }

            if (view is AppCompatImageButton) {
                picasso
                    .load(R.drawable.sample_image)
                    .fit()
                    .centerCrop()
                    .transform(viewShape.getTransformation(this))
                    .apply {
                        view.post { into(view) }
                    }
            }

            viewModel.selectedShape.subscribeBy { selectedShape ->
                val isViewOfSelectedShape = viewShape == selectedShape

                if (view is AppCompatImageButton) {
                    // Animate scale of the shape view.
                    if (isViewOfSelectedShape && view.scaleX != selectedShapeScale) {
                        view.animateScale(
                            target = selectedShapeScale,
                            duration = shapeScaleAnimationDuration
                        )
                    } else if (!isViewOfSelectedShape && view.scaleX != defaultShapeScale) {
                        view.animateScale(
                            target = defaultShapeScale,
                            duration = shapeScaleAnimationDuration
                        )
                    }
                } else {
                    // Animate visibility of the check view.
                    if (isViewOfSelectedShape && !view.isVisible) {
                        view.fadeIn()
                    } else if (!isViewOfSelectedShape) {
                        view.isVisible = false
                    }
                }
            }.addTo(compositeDisposable)
        }

        cardContentView.contentLayout.post {
            cardContentView.contentLayout.addView(shapesLayout)
            compositeDisposable.autoDispose(this)
        }
    }

    private fun initSearch() {
        searchView.init(
            searchView = view.searchView,
            configView = view.searchContent,
        )

        // Animate the search view parent card
        // for pleasant transition.
        view.searchView.addTransitionListener { _, _, newState ->
            when (newState) {
                SearchView.TransitionState.HIDING ->
                    view.searchCardView.fadeOut()

                SearchView.TransitionState.HIDDEN ->
                    view.searchCardView.isVisible = false

                SearchView.TransitionState.SHOWING ->
                    view.searchCardView.fadeIn()

                SearchView.TransitionState.SHOWN -> {
                    view.searchCardView.isVisible = true
                    view.searchCardView.alpha = 1f
                }
            }
        }

        cardContentView.searchConfigLayout.setThrottleOnClickListener {
            view.searchCardView.isVisible = true
            viewModel.searchViewModel.onSearchSummaryClicked()
        }

        viewModel.searchViewModel.state.subscribe { state ->
            cardContentView.searchConfigTextView.text = when (state) {
                is GallerySearchViewModel.State.Applied ->
                    searchSummaryFactory.getSummary(
                        search = state.search,
                        textView = cardContentView.searchConfigTextView,
                    )

                is GallerySearchViewModel.State.Configuring ->
                    if (state.alreadyAppliedSearch != null)
                        searchSummaryFactory.getSummary(
                            search = state.alreadyAppliedSearch,
                            textView = cardContentView.searchConfigTextView,
                        )
                    else
                        getString(R.string.photo_frame_widget_configuration_all_photos_to_show)

                GallerySearchViewModel.State.NoSearch ->
                    getString(R.string.photo_frame_widget_configuration_all_photos_to_show)
            }
        }.autoDispose(this)
    }

    private fun initDate() {
        cardContentView.showDateLayout.setOnClickListener {
            cardContentView.showDateSwitch.toggle()
        }
        cardContentView.showDateSwitch.bindCheckedTwoWay(viewModel.isDateShown, this)
    }

    private fun initButtons() {
        view.cancelButton.setThrottleOnClickListener {
            viewModel.onCancelClicked()
        }
        view.primaryButton.setThrottleOnClickListener {
            viewModel.onDoneClicked()
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribeBy { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            is PhotoFrameWidgetConfigurationViewModel.Event.Finish -> {
                if (event.isConfigurationSuccessful) {
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    )
                }
                finish()
            }
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }
}
