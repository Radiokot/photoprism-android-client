package ua.com.radiokot.photoprism.features.importt.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.features.importt.logic.ImportFilesUseCase
import ua.com.radiokot.photoprism.features.importt.logic.ParseImportIntentUseCase
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile

class ImportDebugActivity : BaseActivity() {
    private val importFilesUseCaseFactory: ImportFilesUseCase.Factory by inject()
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

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaLocationPermissionLauncher?.launch(Unit)

        val files = parseImportIntentUseCaseFactory.get(intent).invoke()

        setContentView(TextView(this).apply {
            setPadding(40, 40, 40, 40)
            text =
                """
                    Action: ${intent.action}
                    
                    Describe: ${
                    files.mapIndexed { index, file ->
                        "#${index} $file"
                    }
                }
                
                    Click to upload
                """.trimIndent()

            setOnClickListener {
                val dialog = ProgressDialog(context)
                importFilesUseCaseFactory.get(
                    files = files.map(ImportableFile::contentUri),
                    uploadToken = System.currentTimeMillis().toString(),
                )
                    .invoke()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { dialog.show() }
                    .subscribeBy(
                        onError = { error ->
                            dialog.dismiss()
                            error.printStackTrace()
                            Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
                        },
                        onComplete = {
                            dialog.dismiss()
                            Toast.makeText(context, "Uploaded successfully", Toast.LENGTH_SHORT)
                                .show()
                        }
                    )
                    .autoDispose(this@ImportDebugActivity)
            }
        })
    }

    private fun onMediaLocationPermissionResult(isGranted: Boolean) {
        Toast.makeText(this, "Can access location: $isGranted", Toast.LENGTH_SHORT).show()
    }
}
