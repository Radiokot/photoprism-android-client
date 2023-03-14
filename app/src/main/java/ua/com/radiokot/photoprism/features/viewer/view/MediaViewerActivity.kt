package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.databinding.ActivityMediaViewerBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.view.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.gallery.view.DownloadProgressView
import ua.com.radiokot.photoprism.features.gallery.view.MediaFileSelectionView
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPageItem
import java.io.File

class MediaViewerActivity : AppCompatActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            linkTo(getScope("session"))
        }
    }

    private lateinit var view: ActivityMediaViewerBinding
    private val viewModel: MediaViewerViewModel by viewModel()
    private val downloadViewModel: DownloadMediaFileViewModel by viewModel()
    private val log = kLogger("MMediaViewerActivity")

    private val viewerPagesAdapter = ItemAdapter<MediaViewerPageItem>()

    private val fileReturnIntentCreator: FileReturnIntentCreator by inject()

    private val mediaFileSelectionView: MediaFileSelectionView by lazy {
        MediaFileSelectionView(
            fragmentManager = supportFragmentManager,
            lifecycleOwner = this
        )
    }
    private val downloadProgressView: DownloadProgressView by lazy {
        DownloadProgressView(
            viewModel = downloadViewModel,
            fragmentManager = supportFragmentManager,
            errorSnackbarView = view.viewPager,
            lifecycleOwner = this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(view.root)

        supportActionBar?.hide()

        val mediaIndex = intent.getIntExtra(MEDIA_INDEX_KEY, -1)
            .takeIf { it >= 0 }
            .checkNotNull {
                "Missing media index"
            }

        val repositoryQuery = intent.getStringExtra(REPO_QUERY_KEY)

        log.debug {
            "onCreate(): creating:" +
                    "\nmediaIndex=$mediaIndex," +
                    "\nrepositoryQuery=$repositoryQuery," +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        viewModel.init(
            downloadViewModel = downloadViewModel,
            repositoryQuery = repositoryQuery,
        )

        subscribeToData()
        subscribeToEvents()

        view.viewPager.post {
            initPager(mediaIndex)
        }
        initButtons()
        initMediaFileSelection()
        downloadProgressView.init()
    }

    private fun initPager(startIndex: Int) {
        with(view.viewPager) {
            val fastAdapter = FastAdapter.with(viewerPagesAdapter).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            adapter = fastAdapter

            // TODO: Endless scrolling

            post {
                setCurrentItem(startIndex, false)
            }
        }
    }

    private fun initButtons() {
        view.shareButton.setOnClickListener {
            viewModel.onShareClicked(
                position = view.viewPager.currentItem,
            )
        }
    }

    private fun subscribeToData() {
        viewModel.isLoading
            .observe(this) { isLoading ->
                log.debug {
                    "subscribeToData(): loading_changed:" +
                            "\nis_loading=$isLoading"
                }

                if (isLoading) {
                    view.progressIndicator.show()
                } else {
                    view.progressIndicator.hide()
                }
            }

        viewModel.itemsList
            .observe(this) {
                if (it != null) {
                    viewerPagesAdapter.setNewList(it)
                }
            }
    }

    private fun subscribeToEvents() {
        viewModel.events.subscribe { event ->
            log.debug {
                "subscribeToEvents(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is MediaViewerViewModel.Event.OpenFileSelectionDialog ->
                    openMediaFilesDialog(
                        files = event.files,
                    )

                is MediaViewerViewModel.Event.ShareDownloadedFile ->
                    shareDownloadedFile(
                        downloadedFile = event.downloadedFile,
                        mimeType = event.mimeType,
                        displayName = event.displayName,
                    )
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.disposeOnDestroy(this)
    }

    private fun initMediaFileSelection() {
        mediaFileSelectionView.init { fileItem ->
            if (fileItem.source != null) {
                viewModel.onFileSelected(fileItem.source)
            }
        }
    }

    private fun openMediaFilesDialog(files: List<GalleryMedia.File>) {
        mediaFileSelectionView.openMediaFileSelectionDialog(
            fileItems = files.map {
                MediaFileListItem(
                    source = it,
                    context = this
                )
            }
        )
    }

    private fun shareDownloadedFile(
        downloadedFile: File,
        mimeType: String,
        displayName: String,
    ) {
        val resultIntent = fileReturnIntentCreator.createIntent(
            fileToReturn = downloadedFile,
            mimeType = mimeType,
            displayName = displayName,
        )

        resultIntent.action = Intent.ACTION_SEND

        log.debug {
            "shareDownloadedFile(): starting_intent:" +
                    "\nintent=$resultIntent" +
                    "\ndownloadedFile=$downloadedFile"
        }

        startActivity(resultIntent)
    }

    companion object {
        private const val MEDIA_INDEX_KEY = "media-index"
        private const val REPO_QUERY_KEY = "repo-query"

        fun getBundle(
            mediaIndex: Int,
            repositoryQuery: String?,
        ) = Bundle().apply {
            putInt(MEDIA_INDEX_KEY, mediaIndex)
            putString(REPO_QUERY_KEY, repositoryQuery)
        }
    }
}