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
import ua.com.radiokot.photoprism.extension.fadeIn
import ua.com.radiokot.photoprism.extension.fadeOut
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.gallery.search.view.GallerySearchView
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.model.PhotoFrameWidgetConfigurationViewModel

class PhotoFrameWidgetConfigurationActivity : BaseActivity() {

    private lateinit var view: ActivityPhotoFrameWidgetConfigurationBinding
    private lateinit var cardContentView: IncludePhotoFrameWidgetConfigurationCardContentBinding
    private val viewModel: PhotoFrameWidgetConfigurationViewModel by viewModel()
    private val widgetsPreferences: PhotoFrameWidgetsPreferences by inject()
    private val updatePhotoFrameWidgetPhotoUseCase: UpdatePhotoFrameWidgetPhotoUseCase by inject()
    private val reloadPhotoFrameWidgetPhotoUseCase: ReloadPhotoFrameWidgetPhotoUseCase by inject()
    private val picasso: Picasso by inject()
    private val shapeScaleAnimationDuration: Int by lazy {
        resources.getInteger(android.R.integer.config_shortAnimTime) / 2
    }
    private val selectedShapeScale = 1.05f
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

        view.cancelButton.setOnClickListener {
            finish()
        }
        view.primaryButton.setOnClickListener {
            widgetsPreferences.setShape(
                widgetId = appWidgetId,
                shape = viewModel.selectedShape.value!!,
            )

            updatePhotoFrameWidgetPhotoUseCase
                .invoke(
                    widgetId = appWidgetId,
                )
                .andThen(
                    reloadPhotoFrameWidgetPhotoUseCase(
                        widgetId = appWidgetId,
                    )
                )
                .subscribeBy(
                    onError = Throwable::printStackTrace,
                    onComplete = {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        )
                        finish()
                    }
                )
                .autoDispose(this)
        }

        cardContentView.contentLayout.post {
            initShapes()
        }
        initSearch()

        subscribeToData()
    }

    private fun initShapes() {
        IncludePhotoFrameWidgetConfigurationShapesBinding.inflate(
            layoutInflater,
            cardContentView.contentLayout
        )

        val sampleImage = R.drawable.sample_image

        cardContentView.contentLayout.forEach { view ->
            val shape = view.tag
                ?.takeIf { it is String }
                ?.runCatching { PhotoFrameWidgetShape.valueOf(toString()) }
                ?.getOrNull()
                ?: return@forEach

            view.setThrottleOnClickListener {
                viewModel.onShapeClicked(shape)
            }

            if (view is AppCompatImageButton) {
                picasso
                    .load(sampleImage)
                    .fit()
                    .transform(shape.getTransformation(this))
                    .into(view)
            }
        }

        viewModel.selectedShape.observe(this) { selectedShape ->
            cardContentView.contentLayout.forEach { view ->
                if (view.tag == null) {
                    return@forEach
                }

                val isViewOfSelectedShape = view.tag == selectedShape.name

                if (view is AppCompatImageButton) {
                    // Animate scale of the shape view.
                    if (isViewOfSelectedShape && view.scaleX != selectedShapeScale) {
                        view.animateScale(
                            target = selectedShapeScale,
                            duration = shapeScaleAnimationDuration
                        )
                    } else if (!isViewOfSelectedShape && view.scaleX != 1f) {
                        view.animateScale(
                            target = 1f,
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
            }
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
            viewModel.searchViewModel.onSearchBarClicked()
        }
    }

    private fun subscribeToData() {

    }
}
