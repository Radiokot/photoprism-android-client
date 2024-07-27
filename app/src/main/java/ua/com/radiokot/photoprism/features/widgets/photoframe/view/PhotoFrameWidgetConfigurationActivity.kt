package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityPhotoFrameWidgetConfigurationBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.util.images.ImageTransformations

class PhotoFrameWidgetConfigurationActivity : BaseActivity() {

    private lateinit var view: ActivityPhotoFrameWidgetConfigurationBinding
    private val updatePhotoFrameWidgetPhotoUseCase: UpdatePhotoFrameWidgetPhotoUseCase by inject()
    private val reloadPhotoFrameWidgetPhotoUseCase: ReloadPhotoFrameWidgetPhotoUseCase by inject()
    private val picasso: Picasso by inject()
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

        view.doneButton.setOnClickListener {
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

        initShapes()
    }

    private fun initShapes() {
        val image = R.drawable.sample_image

        picasso
            .load(image)
            .fit()
            .transform(
                ImageTransformations.roundedCorners(
                    cornerRadiusDp = 8,
                    context = this,
                )
            )
            .into(view.roundedCornersImageView)

        picasso
            .load(image)
            .fit()
            .transform(ImageTransformations.fufa(this))
            .into(view.fufaImageView)

        picasso
            .load(image)
            .fit()
            .transform(ImageTransformations.sasha(this))
            .into(view.sashaImageView)

        picasso
            .load(image)
            .fit()
            .transform(ImageTransformations.leaf(this))
            .into(view.leafImageView)

        picasso
            .load(image)
            .fit()
            .transform(ImageTransformations.buba(this))
            .into(view.bubaImageView)

        picasso
            .load(image)
            .fit()
            .transform(ImageTransformations.heart(this))
            .into(view.heartImageView)

        picasso
            .load(image)
            .fit()
            .centerCrop()
            .transform(ImageTransformations.vSauce)
            .into(view.vsauceImageView)

        picasso
            .load(image)
            .fit()
            .transform(ImageTransformations.nona(this))
            .into(view.nonaImageView)

        picasso
            .load(image)
            .fit()
            .transform(ImageTransformations.gear(this))
            .into(view.gearImageView)

        picasso
            .load(image)
            .fit()
            .centerCrop()
            .transform(ImageTransformations.hSauce)
            .into(view.hsauceImageView)
    }
}
