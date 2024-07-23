package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityPhotoFrameWidgetConfigurationBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetPhotoUseCase

class PhotoFrameWidgetConfigurationActivity : BaseActivity() {

    private lateinit var view: ActivityPhotoFrameWidgetConfigurationBinding
    private val updatePhotoFrameWidgetPhotoUseCase: UpdatePhotoFrameWidgetPhotoUseCase by inject()
    private val reloadPhotoFrameWidgetPhotoUseCase: ReloadPhotoFrameWidgetPhotoUseCase by inject()
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory by inject()
    private val appWidgetId: Int by lazy {
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }

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
        setContentView(view.root)

        view.okButton.setOnClickListener {
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

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
