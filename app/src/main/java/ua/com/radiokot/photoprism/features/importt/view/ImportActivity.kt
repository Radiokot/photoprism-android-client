package ua.com.radiokot.photoprism.features.importt.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.material.color.MaterialColors
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityImportBinding
import ua.com.radiokot.photoprism.databinding.IncludeImportCardContentBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.albums.view.DestinationAlbumSelectionActivity
import ua.com.radiokot.photoprism.features.importt.view.model.ImportViewModel

class ImportActivity : BaseActivity() {
    private val log = kLogger("ImportActivity")

    private lateinit var view: ActivityImportBinding
    private lateinit var cardContentView: IncludeImportCardContentBinding
    private val viewModel: ImportViewModel by viewModel()
    private val permissionsRequestLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            this::onPermissionsResult
        )
    private val albumSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onAlbumSelectionResult
    )

    override val windowBackgroundColor: Int
        get() = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        view = ActivityImportBinding.inflate(layoutInflater)
        cardContentView = IncludeImportCardContentBinding.bind(view.mainCardView)
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
                cardContentView.rationaleDivider.isVisible = true
                cardContentView.rationaleBottomSpace.isVisible = true
            }
        }
        viewModel.isNotificationPermissionRationaleVisible.observe(
            this,
            permissionRationaleObserver(cardContentView.notificationsPermissionRationaleTextView)
        )
        viewModel.isMediaPermissionRationaleVisible.observe(
            this,
            permissionRationaleObserver(cardContentView.mediaPermissionRationaleTextView)
        )

        viewModel.isStartButtonEnabled.observe(this, view.primaryButton::setEnabled)
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

            is ImportViewModel.Event.OpenAlbumSelectionForResult ->
                albumSelectionLauncher.launch(
                    Intent(this, DestinationAlbumSelectionActivity::class.java)
                        .putExtras(
                            DestinationAlbumSelectionActivity.getBundle(
                                selectedAlbums = event.currentlySelectedAlbums,
                            )
                        )
                )
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

    private fun showSummary(summary: ImportViewModel.Summary) {
        cardContentView.summaryItemsLayout.removeAllViews()

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
        addSummaryItem(
            content = summary.albums
                .takeIf(Collection<*>::isNotEmpty)
                ?.joinToString()
                ?: getString(R.string.import_albums_not_selected),
            summary = R.string.albums,
            onClick = viewModel::onAlbumsClicked,
        )
    }

    @SuppressLint("PrivateResource")
    private fun addSummaryItem(
        content: String,
        @StringRes
        summary: Int,
        onClick: (() -> Unit)? = null,
    ) {
        layoutInflater.inflate(
            androidx.preference.R.layout.preference_material,
            cardContentView.summaryItemsLayout,
            false
        ).apply {
            findViewById<ViewGroup>(androidx.preference.R.id.icon_frame).isVisible = false
            findViewById<TextView>(android.R.id.title).setText(summary)
            findViewById<TextView>(android.R.id.summary).text = content

            if (onClick != null) {
                setThrottleOnClickListener {
                    onClick()
                }

                findViewById<ViewGroup>(android.R.id.widget_frame).addView(
                    AppCompatImageView(context).apply {
                        setImageResource(R.drawable.ic_keyboard_arrow_right)
                        imageTintList = ColorStateList.valueOf(
                            MaterialColors.getColor(
                                this,
                                com.google.android.material.R.attr.colorOnSurfaceVariant
                            )
                        )
                    }
                )
            }
        }.also(cardContentView.summaryItemsLayout::addView)
    }

    private fun onPermissionsResult(results: Map<String, Boolean>) =
        viewModel.onPermissionsResult(results)

    private fun onAlbumSelectionResult(result: ActivityResult) {
        val bundle = result.data?.extras
        if (result.resultCode == Activity.RESULT_OK && bundle != null) {
            viewModel.onAlbumSelectionResult(
                selectedAlbums = DestinationAlbumSelectionActivity.getSelectedAlbums(bundle)
            )
        }
    }
}
