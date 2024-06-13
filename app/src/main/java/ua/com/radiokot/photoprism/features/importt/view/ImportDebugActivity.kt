package ua.com.radiokot.photoprism.features.importt.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore.MediaColumns
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

class ImportDebugActivity : BaseActivity() {
    private val importFilesUseCaseFactory: ImportFilesUseCase.Factory by inject()
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

        val uris: Set<Uri> =
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::setOf) ?: emptySet()) +
                    (intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptySet()) +
                    (intent.data?.let(::setOf) ?: emptySet())

        setContentView(TextView(this).apply {
            setPadding(40, 40, 40, 40)
            text =
                """
                    Action: ${intent.action}
                    
                    URIs: ${uris}
                    
                    Describe: ${
                    uris.mapIndexed { index, uri ->
                        "#${index} ${describeFile(uri)}"
                    }
                }
                
                    Click to upload
                """.trimIndent()

            setOnClickListener {
                val dialog = ProgressDialog(context)
                importFilesUseCaseFactory.get(
                    files = uris,
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

    private fun describeFile(uri: Uri): String = try {
        val mimeType = contentResolver.getType(uri)

        contentResolver.query(
            uri,
            arrayOf(MediaColumns.DISPLAY_NAME, MediaColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            check(cursor.moveToNext()) {
                "The result is empty"
            }

            val displayNameIndex = cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)

            return """$mimeType ${cursor.getLong(sizeIndex)} ${
                cursor.getString(
                    displayNameIndex
                )
            }"""
        } ?: "failed"
    } catch (e: Exception) {
        e.printStackTrace()
        "failed"
    }

    private fun onMediaLocationPermissionResult(isGranted: Boolean) {
        Toast.makeText(this, "Can access location: $isGranted", Toast.LENGTH_SHORT).show()
    }
}
