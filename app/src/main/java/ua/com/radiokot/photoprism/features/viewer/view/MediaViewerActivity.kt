package ua.com.radiokot.photoprism.features.viewer.view

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.GenericItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.EventHook
import com.mikepenz.fastadapter.listeners.addClickListener
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMediaViewerBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.*
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.view.DownloadProgressView
import ua.com.radiokot.photoprism.features.gallery.view.MediaFileSelectionView
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem
import ua.com.radiokot.photoprism.features.viewer.view.model.*
import ua.com.radiokot.photoprism.util.CustomTabsHelper
import ua.com.radiokot.photoprism.util.FullscreenInsetsUtil
import java.io.File

class MediaViewerActivity : BaseActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            linkTo(getScope(DI_SCOPE_SESSION))
        }
    }

    private lateinit var view: ActivityMediaViewerBinding
    private val viewModel: MediaViewerViewModel by viewModel()
    private val downloadViewModel: DownloadMediaFileViewModel by viewModel()
    private val videoPlayerCacheViewModel: VideoPlayerCacheViewModel by viewModel()
    private val log = kLogger("MMediaViewerActivity")

    private val viewerPagesAdapter = ItemAdapter<MediaViewerPage>()

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
    private val storagePermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            this::onStoragePermissionResult
        )

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

        viewModel.initOnce(
            downloadViewModel = downloadViewModel,
            repositoryQuery = repositoryQuery,
        )

        // Init before the subscription.
        initPager(mediaIndex, savedInstanceState)

        subscribeToData()
        subscribeToEvents()

        initButtons()
        initMediaFileSelection()
        downloadProgressView.init()
        initFullScreenToggle()
        initCustomTabs()
    }

    private fun initPager(
        startIndex: Int,
        savedInstanceState: Bundle?,
    ) {
        with(view.viewPager) {
            val fastAdapter = FastAdapter.with(viewerPagesAdapter).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

                // Set the required index once, after the data is set.
                if (savedInstanceState == null) {
                    registerAdapterDataObserver(object : AdapterDataObserver() {
                        override fun onChanged() {
                            post {
                                setCurrentItem(startIndex, false)
                            }
                            unregisterAdapterDataObserver(this)
                        }
                    })
                }

                addClickListener(
                    resolveView = { viewHolder: RecyclerView.ViewHolder ->
                        when (viewHolder) {
                            is ImageViewerPage.ViewHolder ->
                                viewHolder.view.photoView
                            is VideoViewerPage.ViewHolder ->
                                viewHolder.view.videoView
                            else ->
                                viewHolder.itemView
                        }
                    },
                    onClick = { _, _, _, _ ->
                        viewModel.onPageClicked()
                    }
                )

                addEventHook(object : EventHook<MediaViewerPage> {
                    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                        if (viewHolder !is VideoViewerPage.ViewHolder) {
                            return null
                        }

                        setUpVideoViewer(viewHolder)

                        return null
                    }
                })
            }

            adapter = fastAdapter

            val endlessScrollListener = object : EndlessRecyclerOnScrollListener(
                // TODO: Should I add loading "footer"?
                footerAdapter = GenericItemAdapter(),
                layoutManager = recyclerView.layoutManager.checkNotNull {
                    "There must be a layout manager at this point"
                },
                visibleThreshold = 6
            ) {
                override fun onLoadMore(currentPage: Int) {
                    if (currentPage == 0) {
                        // Filter out false-triggering.
                        return
                    }

                    log.debug {
                        "onLoadMore(): load_more:" +
                                "\npage=$currentPage"
                    }
                    viewModel.loadMore()
                }
            }
            viewModel.isLoading.observe(this@MediaViewerActivity) { isLoading ->
                if (isLoading) {
                    endlessScrollListener.disable()
                } else {
                    endlessScrollListener.enable()
                }
            }
            recyclerView.addOnScrollListener(endlessScrollListener)
        }
    }

    private fun initButtons() {
        view.shareButton.setOnClickListener {
            viewModel.onShareClicked(
                position = view.viewPager.currentItem,
            )
        }

        view.openInButton.setOnClickListener {
            viewModel.onOpenInClicked(
                position = view.viewPager.currentItem,
            )
        }

        view.downloadButton.setOnClickListener {
            viewModel.onDownloadClicked(
                position = view.viewPager.currentItem,
            )
        }

        view.openInWebViewerButton.setOnClickListener {
            viewModel.onOpenInWebViewerClicked(
                position = view.viewPager.currentItem,
            )
        }

        window.decorView.post {
            val insets = FullscreenInsetsUtil.getForTranslucentSystemBars(window.decorView)

            view.buttonsLayout.layoutParams =
                (view.buttonsLayout.layoutParams as MarginLayoutParams).apply {
                    bottomMargin += insets.bottom
                    leftMargin += insets.left
                    rightMargin += insets.right

                    log.debug {
                        "initButtons(): applied_buttons_insets_margin:" +
                                "\nleft=$leftMargin," +
                                "\nright=$rightMargin," +
                                "\nbottom=$bottomMargin"
                    }
                }
        }
    }

    @Suppress("DEPRECATION")
    private fun initFullScreenToggle() {
        window.decorView.setOnSystemUiVisibilityChangeListener { flags ->
            val isFullScreen =
                flags and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
            viewModel.onFullScreenToggledBySystem(isFullScreen)
        }
    }

    private fun initCustomTabs() {
        CustomTabsHelper.safelyConnectAndInitialize(this)
    }

    private fun setUpVideoViewer(viewHolder: VideoViewerPage.ViewHolder) {
        viewHolder.playerCache = videoPlayerCacheViewModel
        viewHolder.bindToLifecycle(this.lifecycle)

        val view = viewHolder.view
        val playerControlsLayout = viewHolder.playerControlsLayout

        viewModel.areActionsVisible.observe(this@MediaViewerActivity) { areActionsVisible ->
            playerControlsLayout.fadeVisibility(isVisible = areActionsVisible)
        }
        if (viewModel.areActionsVisible.value == true) {
            view.videoView.showController()
        } else {
            view.videoView.hideController()
        }
        playerControlsLayout.clearAnimation()

        window.decorView.post {
            val extraBottomMargin = this.view.buttonsLayout.height +
                    (this.view.buttonsLayout.layoutParams as MarginLayoutParams).bottomMargin
            val extraLeftMargin =
                (this.view.buttonsLayout.layoutParams as MarginLayoutParams).leftMargin
            val extraRightMargin =
                (this.view.buttonsLayout.layoutParams as MarginLayoutParams).rightMargin

            with(view.videoView.findViewById<View>(R.id.player_progress_payout)) {
                layoutParams = (layoutParams as MarginLayoutParams).apply {
                    bottomMargin += extraBottomMargin
                    leftMargin += extraLeftMargin
                    rightMargin += extraRightMargin

                    log.debug {
                        "setUpVideoViewer(): applied_controls_insets_margin:" +
                                "\nleft=$leftMargin," +
                                "\nright=$rightMargin," +
                                "\nbottom=$bottomMargin"
                    }

                }
            }
        }

        viewHolder.fatalPlaybackErrorListener = viewModel::onVideoPlayerFatalPlaybackError
    }

    private fun subscribeToData() {
        viewModel.isLoading.observe(this) { isLoading ->
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

        viewModel.itemsList.observe(this) {
            if (it != null) {
                viewerPagesAdapter.setNewList(it)
            }
        }

        viewModel.areActionsVisible.observe(this) { areActionsVisible ->
            view.buttonsLayout.fadeVisibility(areActionsVisible)
        }

        viewModel.isFullScreen.observe(this) { isFullScreen ->
            if (isFullScreen) {
                hideSystemUI()
                log.debug { "initData(): enabled_full_screen" }
            } else {
                showSystemUI()
                log.debug { "initData(): disabled_full_screen" }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView)
            .systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH

        val uiOptions = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        window.decorView.systemUiVisibility = uiOptions
    }

    @Suppress("DEPRECATION")
    private fun showSystemUI() {
        val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        window.decorView.systemUiVisibility = uiOptions
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

                is MediaViewerViewModel.Event.OpenDownloadedFile ->
                    openDownloadedFile(
                        downloadedFile = event.downloadedFile,
                        mimeType = event.mimeType,
                        displayName = event.displayName,
                    )

                is MediaViewerViewModel.Event.CheckStoragePermission ->
                    checkStoragePermission()

                is MediaViewerViewModel.Event.ShowSuccessfulDownloadMessage ->
                    showSuccessfulDownloadMessage(
                        fileName = event.fileName,
                    )

                is MediaViewerViewModel.Event.ShowMissingStoragePermissionMessage ->
                    showMissingStoragePermissionMessage()

                is MediaViewerViewModel.Event.OpenUrl ->
                    openUrl(
                        url = event.url,
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

    private fun openDownloadedFile(
        downloadedFile: File,
        mimeType: String,
        displayName: String,
    ) {
        val resultIntent = fileReturnIntentCreator.createIntent(
            fileToReturn = downloadedFile,
            mimeType = mimeType,
            displayName = displayName,
        )

        resultIntent.action = Intent.ACTION_VIEW

        log.debug {
            "openDownloadedFile(): starting_intent:" +
                    "\nintent=$resultIntent" +
                    "\ndownloadedFile=$downloadedFile"
        }

        startActivity(resultIntent)
    }

    private fun checkStoragePermission() {
        storagePermissionsLauncher.launch(Unit)
    }

    private fun onStoragePermissionResult(isGranted: Boolean) {
        viewModel.onStoragePermissionResult(isGranted)
    }

    private fun showSuccessfulDownloadMessage(fileName: String) {
        Snackbar.make(
            view.viewPager,
            getString(
                R.string.template_successfully_downloaded_file,
                fileName
            ),
            Snackbar.LENGTH_LONG,
        ).show()
    }

    private fun showMissingStoragePermissionMessage() {
        Snackbar.make(
            view.viewPager,
            getString(R.string.error_storage_permission_is_required),
            Snackbar.LENGTH_SHORT,
        ).show()
    }

    private fun openUrl(url: String) {
        val uri = Uri.parse(url)

        CustomTabsHelper.safelyLaunchUrl(
            this,
            CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setUrlBarHidingEnabled(true)
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
                .build(),
            uri
        )
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