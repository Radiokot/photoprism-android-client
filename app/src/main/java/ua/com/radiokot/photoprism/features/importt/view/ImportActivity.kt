package ua.com.radiokot.photoprism.features.importt.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityImportBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.importt.logic.ImportFilesWorker
import ua.com.radiokot.photoprism.features.importt.logic.ParseImportIntentUseCase
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile

class ImportActivity : BaseActivity() {
    private val log = kLogger("ImportActivity")

    private lateinit var view: ActivityImportBinding
    private val parseImportIntentUseCaseFactory: ParseImportIntentUseCase.Factory by inject()
    private val mediaLocationPermissionLauncher: ActivityResultLauncher<Unit>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            registerForActivityResult(
                ActivityResultContracts.RequestPermission(),
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                this::onMediaLocationPermissionResult
            )
        else
            null

    override val windowBackgroundColor: Int
        get() = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        view = ActivityImportBinding.inflate(layoutInflater)
        setContentView(view.root)

        mediaLocationPermissionLauncher?.launch(Unit)

        val files = parseImportIntentUseCaseFactory.get(intent).invoke()

        addSummaryItem(
            "https://heh",
            R.string.library_root_url
        )
        addSummaryItem(
            content = getString(
                R.string.template_import_files_size,
                resources.getQuantityString(R.plurals.files, files.size, files.size),
                files.sumOf(ImportableFile::size).toDouble() / (1024 * 1024)
            ),
            R.string.import_to_be_uploaded
        )

        view.primaryButton.setOnClickListener {
            // Allow reading the URIs after the activity is finished.
            files.forEach { file ->
                this@ImportActivity.grantUriPermission(
                    packageName,
                    file.contentUri.toUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            WorkManager.getInstance(this@ImportActivity)
                .enqueue(
                    OneTimeWorkRequestBuilder<ImportFilesWorker>()
                        .setInputData(
                            ImportFilesWorker.getInputData(
                                files = files,
                                jsonObjectMapper = get(),
                            )
                        )
                        .addTag(ImportFilesWorker.TAG)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                )
            Toast.makeText(this, "Uploading in background", Toast.LENGTH_SHORT).show()
            finish()
        }

        view.cancelButton.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("PrivateResource")
    private fun addSummaryItem(
        content: String,
        @StringRes
        summary: Int,
    ) {
        val itemLayout =
            layoutInflater.inflate(
                androidx.preference.R.layout.preference_material,
                view.summaryItemsLayout,
                false
            )
        itemLayout.apply {
            findViewById<ViewGroup>(androidx.preference.R.id.icon_frame).isVisible = false
            findViewById<TextView>(android.R.id.title).text = content
            findViewById<TextView>(android.R.id.summary).setText(summary)
        }
        view.summaryItemsLayout.addView(itemLayout)
    }

    private fun onMediaLocationPermissionResult(isGranted: Boolean) {
        Toast.makeText(this, "Can access location: $isGranted", Toast.LENGTH_SHORT).show()
    }
}
