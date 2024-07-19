package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityPhotoFrameWidgetConfigurationBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.model.ViewableAsImage
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase

class PhotoFrameWidgetConfigurationActivity : BaseActivity() {
    private val log = kLogger("PhotoFrameWidgetConfigurationActivity")

    private lateinit var view: ActivityPhotoFrameWidgetConfigurationBinding
    private val picasso: Picasso by inject()
    private val widgetsPreferences: PhotoFrameWidgetsPreferences by inject()
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
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val remoteView = RemoteViews(packageName, R.layout.widget_photo_frame)
            val repository = galleryMediaRepositoryFactory
                .get(
                    SearchConfig.DEFAULT.copy(
                        mediaTypes = setOf(
                            GalleryMedia.TypeName.IMAGE,
                            GalleryMedia.TypeName.RAW,
                        )
                    )
                )

            repository
                .updateIfNotFreshDeferred()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    val randomPhoto = repository.itemsList.random()

                    widgetsPreferences.setPhotoUrl(
                        widgetId = appWidgetId,
                        photoUrl = (randomPhoto.media as ViewableAsImage).getImagePreviewUrl(800)
                    )

                    reloadPhotoFrameWidgetPhotoUseCase(appWidgetId)

                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    )
                    finish()
                }
                .autoDispose(this)
        }

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
