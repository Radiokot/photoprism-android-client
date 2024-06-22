package ua.com.radiokot.photoprism.features.importt.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityImportBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.importt.view.model.ImportViewModel

class ImportActivity : BaseActivity() {
    private val log = kLogger("ImportActivity")

    private lateinit var view: ActivityImportBinding
    private val viewModel: ImportViewModel by viewModel()
    private val permissionsRequestLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            this::onPermissionsResult
        )

    override val windowBackgroundColor: Int
        get() = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        view = ActivityImportBinding.inflate(layoutInflater)
        setContentView(view.root)

        viewModel.initOnce(intent)

        initButtons()

        subscribeToData()
        subscribeToEvents()
    }

    private fun initButtons() {
        view.primaryButton.setOnClickListener {
            viewModel.onStartClicked()
        }

        view.cancelButton.setOnClickListener {
            viewModel.onCancelClicked()
        }
    }

    private fun subscribeToData() {
        viewModel.summary.observe(
            this,
            ::showSummary
        )

        fun permissionRationaleObserver(rationaleView: TextView) = Observer { isVisible: Boolean ->
            rationaleView.isVisible = isVisible
            if (isVisible) {
                view.rationaleDivider.isVisible = true
                view.rationaleBottomSpace.isVisible = true
            }
        }
        viewModel.isNotificationPermissionRationaleVisible.observe(
            this,
            permissionRationaleObserver(view.notificationsPermissionRationaleTextView)
        )
        viewModel.isMediaPermissionRationaleVisible.observe(
            this,
            permissionRationaleObserver(view.mediaPermissionRationaleTextView)
        )
    }

    private fun subscribeToEvents() = viewModel.events.subscribeBy { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            ImportViewModel.Event.Finish ->
                finish()

            ImportViewModel.Event.ShowStartedInBackgroundMessage ->
                Toast.makeText(this, R.string.import_started_in_background, Toast.LENGTH_SHORT)
                    .show()

            is ImportViewModel.Event.RequestPermissions ->
                permissionsRequestLauncher.launch(event.permissions)
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

    private fun showSummary(summary: ImportViewModel.Summary) {
        view.summaryItemsLayout.removeAllViews()

        addSummaryItem(
            content = summary.libraryRootUrl,
            summary = R.string.library_root_url,
        )
        addSummaryItem(
            content = getString(
                R.string.template_import_files_size,
                resources.getQuantityString(R.plurals.files, summary.fileCount, summary.fileCount),
                summary.sizeMb,
            ),
            summary = R.string.import_to_be_uploaded,
        )
    }

    @SuppressLint("PrivateResource")
    private fun addSummaryItem(
        content: String,
        @StringRes
        summary: Int,
    ) {
        layoutInflater.inflate(
            androidx.preference.R.layout.preference_material,
            view.summaryItemsLayout,
            false
        ).apply {
            findViewById<ViewGroup>(androidx.preference.R.id.icon_frame).isVisible = false
            findViewById<TextView>(android.R.id.title).text = content
            findViewById<TextView>(android.R.id.summary).setText(summary)
        }.also(view.summaryItemsLayout::addView)
    }

    private fun onPermissionsResult(results: Map<String, Boolean>) =
        viewModel.onPermissionsResult(results)
}
