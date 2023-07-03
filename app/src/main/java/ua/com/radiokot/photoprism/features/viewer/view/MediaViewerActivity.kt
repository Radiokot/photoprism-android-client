package ua.com.radiokot.photoprism.features.viewer.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.GenericItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.EventHook
import com.mikepenz.fastadapter.listeners.addClickListener
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMediaViewerBinding
import ua.com.radiokot.photoprism.extension.*
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.view.DownloadProgressView
import ua.com.radiokot.photoprism.features.gallery.view.MediaFileSelectionView
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem
import ua.com.radiokot.photoprism.features.viewer.view.model.*
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.util.CustomTabsHelper
import ua.com.radiokot.photoprism.util.FullscreenInsetsUtil
import java.io.File

class MediaViewerActivity : BaseActivity() {
    private lateinit var view: ActivityMediaViewerBinding
    private val viewModel: MediaViewerViewModel by viewModel()
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
            viewModel = viewModel.downloadMediaFileViewModel,
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

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        view = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(view.root)

        supportActionBar?.hide()

        val mediaIndex = intent.getIntExtra(MEDIA_INDEX_KEY, -1)
            .takeIf { it >= 0 }
            .checkNotNull {
                "Missing media index"
            }

        @Suppress("DEPRECATION")
        val repositoryParams: SimpleGalleryMediaRepository.Params =
            requireNotNull(intent.getParcelableExtra(REPO_PARAMS_KEY)) {
                "No repository params specified"
            }
        val areActionsEnabled = intent.getBooleanExtra(ACTIONS_ENABLED_KEY, true)

        log.debug {
            "onCreate(): creating:" +
                    "\nmediaIndex=$mediaIndex," +
                    "\nrepositoryParams=$repositoryParams," +
                    "\nareActionsEnabled=$areActionsEnabled," +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        viewModel.initOnce(
            repositoryParams = repositoryParams,
            areActionsEnabled = areActionsEnabled,
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
        initKeyboardNavigation()
    }

    private fun initPager(
        startIndex: Int,
        savedInstanceState: Bundle?,
    ) = with(view.viewPager) {
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

        registerOnPageChangeCallback(object : OnPageChangeCallback() {
            private var prevSelectedPagePosition = -1

            override fun onPageSelected(position: Int) {
                if (position != prevSelectedPagePosition) {
                    prevSelectedPagePosition = position
                    viewModel.onPageChanged(position)
                }
            }
        })

        // Disable focusability of the inner RecyclerView
        // so it doesn't mess with the arrow keys swipes.
        with(recyclerView) {
            isFocusable = false
            isFocusableInTouchMode = false
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

        view.cancelDownloadButton.setOnClickListener {
            viewModel.onCancelDownloadClicked(
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

    private val keyboardNavigationKeyListener = OnKeyListener { parentView, keyCode, event ->
        log.debug {
            "initKeyboardNavigation(): key_pressed:" +
                    "\ncode=$keyCode," +
                    "\naction=${event.action}"
        }

        // Ignore all the irrelevant keys.
        // Do not intercept Enter on buttons.
        if (keyCode !in setOf(
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_ENTER
            ) || keyCode == KeyEvent.KEYCODE_ENTER && parentView is Button
        ) {
            return@OnKeyListener false
        }

        // Ignore all the irrelevant events, but return true to avoid focus loss.
        if (event.action != KeyEvent.ACTION_UP || !event.hasNoModifiers()) {
            return@OnKeyListener true
        }

        // Swipe pages when pressing left and right arrow buttons.
        // Call page click by pressing Enter (OK).
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                log.debug {
                    "initKeyboardNavigation(): swipe_page_by_key:" +
                            "\nkey=right"
                }

                view.viewPager.setCurrentItem(
                    view.viewPager.currentItem + 1,
                    true
                )
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                log.debug {
                    "initKeyboardNavigation(): swipe_page_by_key:" +
                            "\nkey=left"
                }

                view.viewPager.setCurrentItem(
                    view.viewPager.currentItem - 1,
                    true
                )
            }

            KeyEvent.KEYCODE_ENTER -> {
                log.debug {
                    "initKeyboardNavigation(): click_page_by_enter"
                }

                viewModel.onPageClicked()
            }
        }

        return@OnKeyListener true
    }

    private fun initKeyboardNavigation() = with(view.keyboardNavigationFocusView) {
        setOnKeyListener(keyboardNavigationKeyListener)

        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                log.debug { "initKeyboardNavigation(): focus_view_got_focus" }
            }
        }
    }

    private fun setUpVideoViewer(viewHolder: VideoViewerPage.ViewHolder) {
        viewHolder.playerCache = videoPlayerCacheViewModel
        viewHolder.bindToLifecycle(this.lifecycle)

        val view = viewHolder.view
        val playerControlsView = viewHolder.playerControlsView

        viewModel.areActionsVisible.observe(this@MediaViewerActivity) { areActionsVisible ->
            playerControlsView.root.fadeVisibility(isVisible = areActionsVisible)
        }
        if (viewModel.areActionsVisible.value == true) {
            view.videoView.showController()
        } else {
            view.videoView.hideController()
        }
        playerControlsView.root.clearAnimation()

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

        // Make arrow keys swipes work when play/pause buttons are in focus.
        playerControlsView.exoPlay.setOnKeyListener(keyboardNavigationKeyListener)
        playerControlsView.exoPause.setOnKeyListener(keyboardNavigationKeyListener)
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

        viewModel.cancelDownloadButtonProgressPercent.observe(this) { downloadProgressPercent ->
            view.cancelDownloadButtonProgress.progress = downloadProgressPercent
            view.cancelDownloadButtonProgress.isIndeterminate = downloadProgressPercent < 0
        }

        viewModel.isCancelDownloadButtonVisible.observe(this) { isVisible ->
            if (isVisible) {
                view.cancelDownloadButtonLayout.isVisible = true
                view.cancelDownloadButtonProgress.show()
            } else {
                view.cancelDownloadButtonLayout.isVisible = false
                view.cancelDownloadButtonProgress.hide()
            }
        }

        viewModel.isDownloadButtonVisible.observe(
            this,
            view.downloadButtonLayout::isVisible::set
        )

        viewModel.isDownloadCompletedIconVisible.observe(
            this,
            view.downloadCompletedIcon::isVisible::set
        )
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

                is MediaViewerViewModel.Event.ShowStartedDownloadMessage ->
                    showStartedDownloadMessage(
                        destinationFileName = event.destinationFileName,
                    )

                is MediaViewerViewModel.Event.ShowMissingStoragePermissionMessage ->
                    showMissingStoragePermissionMessage()

                is MediaViewerViewModel.Event.OpenWebViewer ->
                    openWebViewer(
                        url = event.url,
                    )
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.autoDispose(this)
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

    private fun showStartedDownloadMessage(destinationFileName: String) {
        Snackbar.make(
            view.snackbarArea,
            getString(
                R.string.template_started_download_file,
                destinationFileName
            ),
            1000,
        ).show()
    }

    private fun showMissingStoragePermissionMessage() {
        Snackbar.make(
            view.snackbarArea,
            getString(R.string.error_storage_permission_is_required),
            Snackbar.LENGTH_SHORT,
        ).show()
    }

    private fun openWebViewer(url: String) {
        startActivity(
            Intent(this, WebViewActivity::class.java).putExtras(
                WebViewActivity.getBundle(
                    url = url,
                    titleRes = R.string.photoprism_web,
                    pageStartedInjectionScripts = setOf(
                        WebViewInjectionScriptFactory.Script.PHOTOPRISM_AUTO_LOGIN,
                    ),
                    pageFinishedInjectionScripts = setOf(
                        WebViewInjectionScriptFactory.Script.PHOTOPRISM_IMMERSIVE,
                    )
                )
            )
        )
    }

    override fun finish() {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(MEDIA_INDEX_KEY, view.viewPager.currentItem)
        )
        super.finish()
    }

    companion object {
        private const val MEDIA_INDEX_KEY = "media-index"
        private const val REPO_PARAMS_KEY = "repo-params"
        private const val ACTIONS_ENABLED_KEY = "actions-enabled"

        /**
         * @param mediaIndex index of the media to start from
         * @param repositoryParams params of the media repository to view
         * @param areActionsEnabled whether such actions as download, share, etc. are enabled or not.
         */
        fun getBundle(
            mediaIndex: Int,
            repositoryParams: SimpleGalleryMediaRepository.Params,
            areActionsEnabled: Boolean,
        ) = Bundle().apply {
            putInt(MEDIA_INDEX_KEY, mediaIndex)
            putParcelable(REPO_PARAMS_KEY, repositoryParams)
            putBoolean(ACTIONS_ENABLED_KEY, areActionsEnabled)
        }

        /**
         * @return last viewed media index, if there was one.
         */
        fun getResult(result: ActivityResult): Int? =
            result
                .takeIf { it.resultCode == Activity.RESULT_OK }
                ?.data
                ?.getIntExtra(MEDIA_INDEX_KEY, -1)
                ?.takeIf { it >= 0 }
    }
}
