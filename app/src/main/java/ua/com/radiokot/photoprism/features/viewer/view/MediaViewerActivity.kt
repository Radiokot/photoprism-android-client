package ua.com.radiokot.photoprism.features.viewer.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.widget.ActionMenuView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.EventHook
import com.mikepenz.fastadapter.listeners.addClickListener
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.qualifier.named
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMediaViewerBinding
import ua.com.radiokot.photoprism.di.UTC_DATE_TIME_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_DATE_TIME_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.extension.capitalized
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.fadeVisibility
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.extension.recyclerView
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.extension.showOverflowItemIcons
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SendableFile
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.view.DownloadProgressView
import ua.com.radiokot.photoprism.features.gallery.view.MediaFileSelectionView
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryContentLoadingError
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryContentLoadingErrorResources
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileDownloadActionsViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.MediaFileListItem
import ua.com.radiokot.photoprism.features.viewer.slideshow.view.SlideshowActivity
import ua.com.radiokot.photoprism.features.viewer.view.model.FadeEndLivePhotoViewerPage
import ua.com.radiokot.photoprism.features.viewer.view.model.ImageViewerPage
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.SwipeDirection
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoPlayerCacheViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoViewerPage
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.util.FullscreenInsetsCompat
import java.text.DateFormat
import kotlin.math.max
import kotlin.math.roundToInt

class MediaViewerActivity : BaseActivity() {
    private val log = kLogger("MMediaViewerActivity")
    private lateinit var view: ActivityMediaViewerBinding
    private val viewModel: MediaViewerViewModel by viewModel()
    private val videoPlayerCacheViewModel: VideoPlayerCacheViewModel by viewModel()
    private val fileReturnIntentCreator: FileReturnIntentCreator by inject()
    private val dateTimeDateFormat: DateFormat by inject(named(UTC_DATE_TIME_DATE_FORMAT))
    private val dateTimeYearDateFormat: DateFormat by inject(named(UTC_DATE_TIME_YEAR_DATE_FORMAT))

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
            viewModel = viewModel,
            fragmentManager = supportFragmentManager,
            errorSnackbarView = view.viewPager,
            lifecycleOwner = this
        )
    }
    private val storagePermissionRequestLauncher =
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
    private val isPageIndicatorEnabled: Boolean by lazy {
        intent.getBooleanExtra(IS_PAGE_INDICATOR_ENABLED_KEY, false)
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
        val staticSubtitle = intent.getStringExtra(STATIC_SUBTITLE_KEY)

        log.debug {
            "onCreate(): creating:" +
                    "\nmediaIndex=$mediaIndex," +
                    "\nrepositoryParams=$repositoryParams," +
                    "\nareActionsEnabled=$areActionsEnabled," +
                    "\nisPageIndicatorEnabled=$isPageIndicatorEnabled" +
                    "\nstaticSubtitle=$staticSubtitle," +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        viewModel.initOnce(
            repositoryParams = repositoryParams,
            areActionsEnabled = areActionsEnabled,
            isPageIndicatorEnabled = isPageIndicatorEnabled,
            staticSubtitle = staticSubtitle,
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

        var lastSelectedPageId = -1L
        var lastSelectedPagePosition = -1

        fastAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                // Detect changing the item at the same position.
                // For example, when an item gets deleted but there are more.
                val currentPosition = currentItem
                if (currentPosition == lastSelectedPagePosition
                    && lastSelectedPageId != fastAdapter.getItemId(currentPosition)
                ) {
                    viewModel.onPageChanged(currentPosition)
                }
            }
        })

        registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position != lastSelectedPagePosition) {
                    lastSelectedPagePosition = position
                    lastSelectedPageId = fastAdapter.getItemId(position)
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

        if (isPageIndicatorEnabled) {
            view.dotsIndicator.attachTo(this)
        }
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
            val insets = FullscreenInsetsCompat.getForTranslucentSystemBars(window.decorView)

            buttonsLayout.updateLayoutParams {
                this as MarginLayoutParams

                bottomMargin += insets.bottom

                // Adjust horizontal margins by the equal value
                // for proper horizontal centering against the video.
                val horizontalInset = max(insets.left, insets.right)
                leftMargin += horizontalInset
                rightMargin += horizontalInset

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

    private val keyboardNavigationKeyListener = OnKeyListener { parentView, keyCode, event ->
        log.debug {
            "initKeyboardNavigation(): key_pressed:" +
                    "\ncode=$keyCode," +
                    "\naction=${event.action}"
        }

        // Ignore all the irrelevant keys.
        // Do not intercept Enter on play/pause button.
        if (keyCode !in setOf(
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER,
            ) || keyCode in setOf(
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER
            ) && parentView.id == androidx.media3.ui.R.id.exo_play_pause
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
                if (parentView.id == androidx.media3.ui.R.id.exo_play_pause) {
                    log.debug {
                        "initKeyboardNavigation(): focus_keyboard_navigation_view_by_key:" +
                                "\nkey=up"
                    }

                    view.keyboardNavigationFocusView.requestFocus(View.FOCUS_UP)
                } else if (view.toolbar.isVisible) {
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

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                log.debug {
                    "initKeyboardNavigation(): focus_buttons:" +
                            "\nkey=down"
                }

                view.buttonsLayout.requestFocus(View.FOCUS_DOWN)
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
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.media_viewer, menu)

        menu.showOverflowItemIcons(isBottomBar = false)

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

        val actionItems = listOf(
            menu.findItem(R.id.archive),
            menu.findItem(R.id.delete),
            menu.findItem(R.id.is_private),
        )
        viewModel.areActionsVisible.observe(this) { areActionsVisible ->
            actionItems.forEach { it.isVisible = areActionsVisible }
        }

        with(menu.findItem(R.id.is_private)) {
            viewModel.isPrivate.observe(this@MediaViewerActivity) { isPrivate ->
                isChecked = isPrivate
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

        R.id.archive -> {
            viewModel.onArchiveClicked(
                position = view.viewPager.currentItem
            )
            true
        }

        R.id.delete -> {
            viewModel.onDeleteClicked(
                position = view.viewPager.currentItem
            )
            true
        }

        R.id.is_private -> {
            viewModel.onPrivateClicked(
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

                playerControlsView.root.updateLayoutParams {
                    this as MarginLayoutParams

                    // Adjust the bottom margin and make the top one match it
                    // for proper vertical centering of play/pause and buffering progress.
                    bottomMargin += extraBottomMargin
                    topMargin = bottomMargin

                    leftMargin += extraLeftMargin
                    rightMargin += extraRightMargin

                    log.debug {
                        "setUpVideoViewer(): applied_controls_insets_margin:" +
                                "\ntop=$topMargin," +
                                "\nleft=$leftMargin," +
                                "\nright=$rightMargin," +
                                "\nbottom=$bottomMargin"
                    }
                }
            }

            // Make arrow keys swipes work when play/pause buttons are in focus.
            playerControlsView.exoPlayPause.setOnKeyListener(keyboardNavigationKeyListener)
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

        viewModel.isPageIndicatorVisible.observe(
            this,
            view.dotsIndicator::fadeVisibility
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

        viewModel.cancelDownloadButtonProgressPercent.observe(this) { percent ->
            if (percent < 0) {
                view.cancelDownloadButtonProgress.isIndeterminate = true
            } else {
                val wasIndeterminate = view.cancelDownloadButtonProgress.isIndeterminate
                view.cancelDownloadButtonProgress.isIndeterminate = false
                view.cancelDownloadButtonProgress.setProgressCompat(percent, !wasIndeterminate)
            }
        }

        Transformations.distinctUntilChanged(viewModel.isCancelDownloadButtonVisible)
            .observe(this) { isVisible ->
                if (isVisible) {
                    view.cancelDownloadButtonLayout.isVisible = true
                    view.cancelDownloadButtonProgress.show()
                    view.cancelDownloadButtonProgress.progress = 0
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

        viewModel.subtitle.observe(this) { subtitle ->
            view.toolbar.subtitle = when (subtitle) {
                is MediaViewerViewModel.SubtitleValue.DateTime ->
                    if (subtitle.withYear)
                        dateTimeYearDateFormat.format(subtitle.localDate).capitalized()
                    else
                        dateTimeDateFormat.format(subtitle.localDate).capitalized()

                is MediaViewerViewModel.SubtitleValue.Static ->
                    subtitle.value
            }
        }

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
        viewModel.mediaFileDownloadActionsEvents.observeOnMain().subscribe(this) { event ->
            log.debug {
                "subscribeToEvents(): received_media_files_actions_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is MediaFileDownloadActionsViewModel.Event.OpenDownloadedFile ->
                    openDownloadedFile(event.file)

                MediaFileDownloadActionsViewModel.Event.RequestStoragePermission ->
                    requestStoragePermission()

                is MediaFileDownloadActionsViewModel.Event.ReturnDownloadedFiles ->
                    error("Unsupported event")

                is MediaFileDownloadActionsViewModel.Event.ShareDownloadedFiles ->
                    shareDownloadedFiles(event.files)

                MediaFileDownloadActionsViewModel.Event.ShowFilesDownloadedMessage ->
                    error("Unsupported event")

                MediaFileDownloadActionsViewModel.Event.ShowMissingStoragePermissionMessage ->
                    showMissingStoragePermissionMessage()
            }

            log.debug {
                "subscribeToEvents(): handled_media_files_actions_event:" +
                        "\nevent=$event"
            }
        }

        viewModel.events.subscribe(this) { event ->
            log.debug {
                "subscribeToEvents(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                is MediaViewerViewModel.Event.OpenFileSelectionDialog ->
                    openMediaFilesDialog(
                        files = event.files,
                    )

                is MediaViewerViewModel.Event.RequestStoragePermission ->
                    requestStoragePermission()

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

                MediaViewerViewModel.Event.Finish ->
                    finish()

                MediaViewerViewModel.Event.OpenDeletingConfirmationDialog ->
                    openDeletingConfirmationDialog()

                is MediaViewerViewModel.Event.ShowFloatingError ->
                    showFloatingError(event.error)
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }
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

    private fun shareDownloadedFiles(
        files: List<SendableFile>,
    ) {
        val resultIntent = fileReturnIntentCreator.createIntent(files)

        log.debug {
            "shareDownloadedFiles(): starting_intent:" +
                    "\nintent=$resultIntent," +
                    "\nfilesCount=${files.size}"
        }

        startActivity(
            Intent.createChooser(
                resultIntent,
                getString(R.string.share),
            )
        )

        viewModel.onDownloadedMediaFilesShared()
    }

    private fun openDownloadedFile(sendableFile: SendableFile) {
        val resultIntent = fileReturnIntentCreator.createIntent(listOf(sendableFile)).also {
            it.action = Intent.ACTION_VIEW
        }

        log.debug {
            "openDownloadedFile(): starting_intent:" +
                    "\nintent=$resultIntent" +
                    "\ndownloadedFile=$sendableFile"
        }

        startActivity(resultIntent)
    }

    private fun requestStoragePermission() {
        storagePermissionRequestLauncher.launch(Unit)
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

    private fun showFloatingError(error: GalleryContentLoadingError) {
        Snackbar.make(
            view.snackbarArea,
            GalleryContentLoadingErrorResources.getMessage(
                error = error,
                context = this,
            ),
            Snackbar.LENGTH_SHORT
        )
            .setAction(R.string.try_again) { viewModel.onFloatingErrorRetryClicked() }
            .show()
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

    private fun openDeletingConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.media_viewer_deleting_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.onDeletingConfirmed()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
        private const val STATIC_SUBTITLE_KEY = "static-subtitle"
        private const val IS_PAGE_INDICATOR_ENABLED_KEY = "is-page-indicator-enabled"

        private val SWIPE_TO_DISMISS_DIRECTIONS = setOf(
            SwipeDirection.DOWN,
            SwipeDirection.UP,
        )

        /**
         * @param mediaIndex index of the media to start from
         * @param repositoryParams params of the media repository to view
         * @param areActionsEnabled whether such actions as download, share, etc. are enabled or not
         * @param isPageIndicatorEnabled whether the dot page indicator is visible or not
         * @param staticSubtitle if set, will be shown in subtitle
         */
        fun getBundle(
            mediaIndex: Int,
            repositoryParams: SimpleGalleryMediaRepository.Params,
            areActionsEnabled: Boolean = true,
            isPageIndicatorEnabled: Boolean = false,
            staticSubtitle: String? = null,
        ) = Bundle().apply {
            putInt(MEDIA_INDEX_KEY, mediaIndex)
            putParcelable(REPO_PARAMS_KEY, repositoryParams)
            putBoolean(ACTIONS_ENABLED_KEY, areActionsEnabled)
            putBoolean(IS_PAGE_INDICATOR_ENABLED_KEY, isPageIndicatorEnabled)
            putString(STATIC_SUBTITLE_KEY, staticSubtitle)
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
