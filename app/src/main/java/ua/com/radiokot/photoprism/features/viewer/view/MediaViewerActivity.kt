package ua.com.radiokot.photoprism.features.viewer.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.ActionMenuView
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.EventHook
import com.mikepenz.fastadapter.listeners.addClickListener
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
import ua.com.radiokot.photoprism.features.viewer.slideshow.view.SlideshowActivity
import ua.com.radiokot.photoprism.features.viewer.view.model.*
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.util.CustomTabsHelper
import ua.com.radiokot.photoprism.util.FullscreenInsetsUtil
import java.io.File
import kotlin.math.roundToInt

class MediaViewerActivity : BaseActivity() {
    private val log = kLogger("MMediaViewerActivity")
    private lateinit var view: ActivityMediaViewerBinding
    private val viewModel: MediaViewerViewModel by viewModel()
    private val videoPlayerCacheViewModel: VideoPlayerCacheViewModel by viewModel()
    private val fileReturnIntentCreator: FileReturnIntentCreator by inject()

    private val viewerPagesAdapter = ItemAdapter<MediaViewerPage>()
    private lateinit var toolbarBackButton: ImageButton
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
    private val slideshowLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onSlideshowResult,
    )
    private val swipeToDismissHandler: SwipeToDismissHandler by lazy {
        SwipeToDismissHandler(
            swipeView = view.root,
            onDismiss = ::finish,
            onSwipeViewMove = { translationY, _ ->
                if (translationY != 0.0f) {
                    onSwipeToDismissGoing()
                }
            },
        ).apply {
            distanceThreshold =
                resources.getDimensionPixelSize(R.dimen.swipe_to_dismiss_distance_threshold)
        }
    }
    private val swipeDirectionDetector: SwipeDirectionDetector by lazy {
        SwipeDirectionDetector(this)
    }
    private val zoomableView: ZoomableView?
        get() = view.viewPager.recyclerView
            .findViewHolderForAdapterPosition(view.viewPager.currentItem) as? ZoomableView
    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }

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
        initToolbar()

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
        // Reset the image view size until it is obtained.
        viewModel.imageViewSize = Size(0, 0)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            // Image view size is the window size multiplied by a zoom factor.
            viewModel.imageViewSize = Size(
                (window.decorView.width * 1.5).roundToInt(),
                (window.decorView.height * 1.5).roundToInt()
            )
        }

        val fastAdapter = FastAdapter.with(viewerPagesAdapter).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            // Set the required index once, after the data is set.
            if (savedInstanceState == null) {
                registerAdapterDataObserver(object : AdapterDataObserver() {
                    override fun onChanged() {
                        recyclerView.scrollToPosition(startIndex)
                        unregisterAdapterDataObserver(this)
                    }
                })
            }

            addClickListener(
                resolveView = { null },
                resolveViews = { viewHolder: ViewHolder ->
                    when (viewHolder) {
                        is FadeEndLivePhotoViewerPage.ViewHolder ->
                            listOf(viewHolder.view.videoView, viewHolder.view.photoView)

                        is VideoViewerPage.ViewHolder ->
                            listOf(viewHolder.view.videoView)

                        is ImageViewerPage.ViewHolder ->
                            listOf(viewHolder.view.photoView)

                        else ->
                            listOf(viewHolder.itemView)
                    }
                },
                onClick = { _, _, _, _ ->
                    viewModel.onPageClicked()
                }
            )

            addEventHook(object : EventHook<MediaViewerPage> {
                override fun onBind(viewHolder: ViewHolder): View? {
                    if (viewHolder is VideoPlayerViewHolder) {
                        setUpVideoViewer(viewHolder)
                    }

                    return null
                }
            })
        }

        adapter = fastAdapter

        recyclerView.addOnScrollListener(ViewerEndlessScrollListener(
            recyclerView = recyclerView,
            isLoadingLiveData = viewModel.isLoading,
            visibleThreshold = 6,
            onLoadMore = { currentPage ->
                log.debug {
                    "onLoadMore(): load_more:" +
                            "\npage=$currentPage"
                }

                viewModel.loadMore()
            }
        ))

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

        // Add a margin between pages for more pleasant swipe.
        setPageTransformer(
            MarginPageTransformer(
                resources.getDimensionPixelSize(R.dimen.media_viewer_page_margin)
            )
        )
    }

    private fun initButtons() {
        view.shareButton.setThrottleOnClickListener {
            viewModel.onShareClicked(
                position = view.viewPager.currentItem,
            )
        }

        view.downloadButton.setThrottleOnClickListener {
            viewModel.onDownloadClicked(
                position = view.viewPager.currentItem,
            )
        }

        view.cancelDownloadButton.setThrottleOnClickListener {
            viewModel.onCancelDownloadClicked(
                position = view.viewPager.currentItem,
            )
        }

        view.favoriteButton.setThrottleOnClickListener {
            viewModel.onFavoriteClicked(
                position = view.viewPager.currentItem,
            )
        }

        view.buttonsLayout.doOnPreDraw { buttonsLayout ->
            val insets = FullscreenInsetsUtil.getForTranslucentSystemBars(window.decorView)

            buttonsLayout.updateLayoutParams {
                this as MarginLayoutParams

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
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER,
            ) || keyCode in setOf(
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER
            ) && parentView is Button
        ) {
            log.debug {
                "initKeyboardNavigation(): press_ignored"
            }

            return@OnKeyListener false
        }

        // Ignore all the irrelevant events, but return true to avoid focus loss.
        if (event.action != KeyEvent.ACTION_DOWN || !event.hasNoModifiers()) {
            return@OnKeyListener true
        }

        // Swipe pages when pressing left and right arrow buttons.
        // Call page click by pressing Enter (OK).
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (view.toolbar.isVisible) {
                    log.debug {
                        "initKeyboardNavigation(): focus_toolbar_back_by_key:" +
                                "\nkey=up"
                    }

                    focusToolbarBackButton()
                }
            }

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

            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
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

        requestFocus()
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
    }

    private fun focusToolbarBackButton() = with(toolbarBackButton) {
        isFocusableInTouchMode = true
        requestFocus(View.FOCUS_UP)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.media_viewer, menu)

        (menu as? MenuBuilder)?.apply {
            // Enable icons for overflow menu items.
            setOptionalIconsVisible(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Apply horizontal margin for overflow menu item icons for more pleasant look.
                val iconMarginHorizontal =
                    resources.getDimensionPixelSize(R.dimen.menu_icon_margin_horizontal)
                visibleItems.forEach { menuItem ->
                    if (!menuItem.requestsActionButton()) {
                        menuItem.icon = InsetDrawable(
                            menuItem.icon,
                            iconMarginHorizontal, 0, iconMarginHorizontal, 0
                        )
                    }
                }
            }
        }

        // Keyboard navigation focus workarounds.
        with(view.toolbar) {
            val unFocusOnDownKeyListener = OnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    with(toolbarBackButton) {
                        isFocusable = false
                        isFocusableInTouchMode = false
                        view.keyboardNavigationFocusView.requestFocus(View.FOCUS_DOWN)
                    }
                    true
                } else {
                    false
                }
            }

            post {
                // Make all the toolbar views not focusable
                // to not mess with the keyboard navigation.
                forEach { toolbarView ->
                    toolbarView.isFocusable = false

                    // Apply un-focusing on down key press for the back button
                    // and for visible menu items.
                    if (toolbarView is ImageButton) {
                        // Back button is an ImageButton.
                        toolbarBackButton = toolbarView
                        toolbarView.setOnKeyListener(unFocusOnDownKeyListener)
                    } else if (toolbarView is ActionMenuView) {
                        // Visible menu item is an ActionMenuItemView
                        // inside an ActionMenuView.
                        toolbarView[0].setOnKeyListener(unFocusOnDownKeyListener)
                    }
                }
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.start_slideshow -> {
            viewModel.onStartSlideshowClicked(
                position = view.viewPager.currentItem
            )
            true
        }

        R.id.open_in -> {
            viewModel.onOpenInClicked(
                position = view.viewPager.currentItem
            )
            true
        }

        R.id.open_in_web_viewer -> {
            viewModel.onOpenInWebViewerClicked(
                position = view.viewPager.currentItem
            )
            true
        }

        else ->
            super.onOptionsItemSelected(item)
    }

    private fun setUpVideoViewer(viewHolder: VideoPlayerViewHolder) {
        viewHolder.playerCache = videoPlayerCacheViewModel
        viewHolder.bindPlayerToLifecycle(this@MediaViewerActivity.lifecycle)

        val playerControlsView = viewHolder.playerControlsLayout

        if (playerControlsView != null) {
            viewModel.areActionsVisible.observe(this@MediaViewerActivity) { areActionsVisible ->
                // Use fade animation rather than videoPlayer controller visibility methods
                // for consistency.
                playerControlsView.root.clearAnimation()
                playerControlsView.root.fadeVisibility(isVisible = areActionsVisible)
            }

            // Apply insets.
            this.view.buttonsLayout.doOnNextLayout { buttonsLayout ->
                val buttonsLayoutParams = buttonsLayout.layoutParams as MarginLayoutParams

                val extraBottomMargin = buttonsLayout.height + buttonsLayoutParams.bottomMargin
                val extraLeftMargin = buttonsLayoutParams.leftMargin
                val extraRightMargin = buttonsLayoutParams.rightMargin

                playerControlsView.playerProgressLayout.updateLayoutParams {
                    this as MarginLayoutParams

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

            // Make arrow keys swipes work when play/pause buttons are in focus.
            playerControlsView.exoPlay.setOnKeyListener(keyboardNavigationKeyListener)
            playerControlsView.exoPause.setOnKeyListener(keyboardNavigationKeyListener)
        }

        viewHolder.setOnFatalPlaybackErrorListener(viewModel::onVideoPlayerFatalPlaybackError)
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

        viewModel.areActionsVisible.observe(
            this,
            view.buttonsLayout::fadeVisibility
        )

        viewModel.isToolbarVisible.observe(
            this,
            view.toolbar::fadeVisibility
        )

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

        viewModel.title.observe(this) { title ->
            // Delay setting the title to avoid weird behaviour
            // on activity init.
            view.toolbar.post {
                setTitle(title)
            }
        }
        viewModel.subtitle.observe(
            this,
            view.toolbar::setSubtitle
        )

        viewModel.isFavorite.observe(this) { isFavorite ->
            view.favoriteButton.contentDescription =
                getString(
                    if (isFavorite)
                        R.string.remove_from_favorites
                    else
                        R.string.add_to_favorites
                )
            ViewCompat.setTooltipText(view.favoriteButton, view.favoriteButton.contentDescription)
            view.favoriteButton.icon =
                ContextCompat.getDrawable(
                    this,
                    if (isFavorite)
                        R.drawable.ic_favorite_filled
                    else
                        R.drawable.ic_favorite
                )
        }
    }

    private fun hideSystemUI() = with(windowInsetsController) {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemUI() =
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

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

                is MediaViewerViewModel.Event.OpenSlideshow ->
                    openSlideshow(
                        mediaIndex = event.mediaIndex,
                        repositoryParams = event.repositoryParams,
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

        log.debug {
            "shareDownloadedFile(): starting_intent:" +
                    "\nintent=$resultIntent" +
                    "\ndownloadedFile=$downloadedFile"
        }

        startActivity(Intent.createChooser(resultIntent, getString(R.string.share)))
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
        ).also {
            it.action = Intent.ACTION_VIEW
        }

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

    private fun openSlideshow(
        mediaIndex: Int,
        repositoryParams: SimpleGalleryMediaRepository.Params,
    ) {
        slideshowLauncher.launch(
            Intent(this, SlideshowActivity::class.java).putExtras(
                SlideshowActivity.getBundle(
                    mediaIndex = mediaIndex,
                    repositoryParams = repositoryParams,
                )
            )
        )
    }

    // Swipe to dismiss happens here.
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        swipeDirectionDetector.handleTouchEvent(event)

        val zoomableView = this.zoomableView
        val isZoomed = zoomableView?.isZoomed == true

        // Do not allow page swipe if currently viewing a zoomed photo
        // unless it can't be panned further.
        // There is a handler for this in PhotoViewAttacher:120, but it is faulty.
        view.viewPager.isUserInputEnabled =
            zoomableView == null
                    || !isZoomed
                    || swipeDirectionDetector.detectedDirection == SwipeDirection.RIGHT
                    && !zoomableView.canPanHorizontally(-1)
                    || swipeDirectionDetector.detectedDirection == SwipeDirection.LEFT
                    && !zoomableView.canPanHorizontally(1)

        // Do not allow swipe to dismiss if currently viewing a zoomed photo.
        if (!isZoomed && swipeToDismissHandler.shouldHandleTouch(event)) {
            if (event.action != MotionEvent.ACTION_MOVE) {
                // When not dragging, let the system dispatch the event
                // but send it to the handler in order to prepare it
                // for possible further swipe.
                swipeToDismissHandler.onTouch(view.root, event)
            } else if (swipeDirectionDetector.detectedDirection in SWIPE_TO_DISMISS_DIRECTIONS) {
                // When dragging and the swipe in required direction is detected,
                // dispatch further touch events to the handler.
                return swipeToDismissHandler.onTouch(view.root, event)
            }
        }

        return super.dispatchTouchEvent(event)
    }

    private fun onSwipeToDismissGoing() {
        // Hide system UI and instantly hide all the controls
        // for the finish animation to be smooth.
        // If the swipe is cancelled, visibility could be returned by a tap.
        hideSystemUI()
        view.toolbar.alpha = 0f
        view.buttonsLayout.alpha = 0f
    }

    private fun onSlideshowResult(result: ActivityResult) {
        val lastViewedMediaIndex = SlideshowActivity.getResult(result)
            ?: return

        view.viewPager.setCurrentItem(lastViewedMediaIndex, false)

        // Show system UI on this screen
        // as the user just have done a swipe make it appear.
        showSystemUI()
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
        private val SWIPE_TO_DISMISS_DIRECTIONS = setOf(
            SwipeDirection.DOWN,
            SwipeDirection.UP,
        )

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
