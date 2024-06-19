package ua.com.radiokot.photoprism.features.importt.view

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.net.toUri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityImportBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.importt.logic.ImportFilesWorker
import ua.com.radiokot.photoprism.features.importt.logic.ParseImportIntentUseCase

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

        view.debugTextView.text =
            """
                    Action: ${intent.action}
                    
                    Describe: ${
                files.mapIndexed { index, file ->
                    "#${index} $file"
                }
            }
                """.trimIndent()

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

    private fun onMediaLocationPermissionResult(isGranted: Boolean) {
        Toast.makeText(this, "Can access location: $isGranted", Toast.LENGTH_SHORT).show()
    }
}
