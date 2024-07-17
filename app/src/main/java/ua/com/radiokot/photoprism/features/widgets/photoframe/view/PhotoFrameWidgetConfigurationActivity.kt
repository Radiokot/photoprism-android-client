package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import com.squareup.picasso.Callback
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

class PhotoFrameWidgetConfigurationActivity : BaseActivity() {
    private val log = kLogger("PhotoFrameWidgetConfigurationActivity")

    private lateinit var view: ActivityPhotoFrameWidgetConfigurationBinding
    private val picasso: Picasso by inject()
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
                    val newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val portraitWidth =
                        newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    val landsWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
                    val landsHeight =
                        newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                    val portraitHeight =
                        newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                    val orientation = resources.configuration.orientation

                    val width =
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                            landsWidth
                        else
                            portraitWidth

                    val height =
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                            landsHeight
                        else
                            portraitHeight

                    val pxWidth = width * resources.displayMetrics.density
                    val pxHeight = height * resources.displayMetrics.density

                    log.debug {
                        "onCreate(): creating_widget:" +
                                "\nimageWidth=$pxWidth," +
                                "\nimageHeight=$pxHeight"
                    }

                    picasso
                        .load((randomPhoto.media as ViewableAsImage).getImagePreviewUrl(800))
                        .resize(pxWidth.toInt(), pxHeight.toInt())
                        .centerCrop()
                        .into(remoteView, R.id.photo_image_view, intArrayOf(appWidgetId),
                            object : Callback {
                                override fun onSuccess() {
                                    remoteView.setInt(
                                        R.id.photo_image_view,
                                        "setBackgroundResource",
                                        android.R.color.transparent
                                    )

                                    appWidgetManager.partiallyUpdateAppWidget(
                                        appWidgetId,
                                        remoteView
                                    )
                                }

                                override fun onError(e: Exception) {
                                    log.error(e) { "failed_loading_image" }
                                }
                            })

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
