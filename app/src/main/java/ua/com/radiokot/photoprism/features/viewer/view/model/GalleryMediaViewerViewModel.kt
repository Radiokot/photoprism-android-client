package ua.com.radiokot.photoprism.features.viewer.view.model

import android.util.Size
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryContentLoadingError
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaDownloadActionsViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaDownloadActionsViewModelDelegate
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaRemoteActionsViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaRemoteActionsViewModelDelegate
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class GalleryMediaViewerViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val galleryMediaDownloadActionsViewModel: GalleryMediaDownloadActionsViewModelDelegate,
    private val galleryMediaRemoteActionsViewModel: GalleryMediaRemoteActionsViewModelDelegate,
    private val galleryPreferences: GalleryPreferences,
    private val webUrlFactory: MediaWebUrlFactory,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) : ViewModel(),
    GalleryMediaDownloadActionsViewModel by galleryMediaDownloadActionsViewModel,
    GalleryMediaRemoteActionsViewModel by galleryMediaRemoteActionsViewModel {

    private val log = kLogger("MediaViewerVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository
    private var isInitialized = false
    private var areActionsEnabled = false
    private var isPageIndicatorEnabled = false
    private var staticSubtitle: String? = null
    private val currentLocalDate = LocalDate()
    private var albumUid: String? = null

    // Media that turned to be not viewable.
    private val afterAllNotViewableMedia = mutableSetOf<GalleryMedia>()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPage>?> = MutableLiveData(null)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOnMain()
    val areActionsVisible: MutableLiveData<Boolean> = MutableLiveData(true)
    val isPageIndicatorVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isToolbarVisible: MutableLiveData<Boolean> = MutableLiveData(true)
    val isFullScreen: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isCancelDownloadButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadCompletedIconVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val title: MutableLiveData<String> = MutableLiveData()
    val subtitle: MutableLiveData<SubtitleValue> = MutableLiveData()
    val isFavorite: MutableLiveData<Boolean> = MutableLiveData()
    val isPrivate: MutableLiveData<Boolean> = MutableLiveData()
    val canRemoveFromAlbum: Boolean
        get() = albumUid != null

    /**
     * Size of the image viewing area in px.
     * Used to load previews of suitable quality.
     * Must be set by the view.
     */
    var imageViewSize: Size = Size(0, 0)
        set(value) {
            val previous = field
            field = value
            if (previous != value) {
                log.debug {
                    "imageViewSize::set(): broadcasting_after_value_change:" +
                            "\nvalue=$value"
                }

                broadcastItemsFromRepository()
            }
        }

    /**
     * From 0 to 100, negative for indeterminate.
     */
    val cancelDownloadButtonProgressPercent: MutableLiveData<Int> = MutableLiveData(-1)

    init {
        // Make download button visibility opposite to the cancel download button.
        isCancelDownloadButtonVisible.observeForever { isCancelDownloadButtonVisible ->
            isDownloadButtonVisible.value = !isCancelDownloadButtonVisible
        }
    }

    fun initOnce(
        repositoryParams: SimpleGalleryMediaRepository.Params,
        areActionsEnabled: Boolean,
        isPageIndicatorEnabled: Boolean,
        staticSubtitle: String?,
        albumUid: String?,
    ) {
        if (isInitialized) {
            return
        }

        galleryMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)
            .checkNotNull {
                "The repository must be created beforehand"
            }
        subscribeToRepository()

        this.areActionsEnabled = areActionsEnabled
        this.isPageIndicatorEnabled = isPageIndicatorEnabled
        this.staticSubtitle = staticSubtitle
        this.albumUid = albumUid

        initControlsVisibility()

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\nrepositoryParam=$repositoryParams," +
                    "\nareActionsEnabled=$areActionsEnabled," +
                    "\nisPageIndicatorEnabled=$isPageIndicatorEnabled," +
                    "\nstaticSubtitle=$staticSubtitle," +
                    "\nalbumUid=$albumUid"
        }

        update()
    }

    private fun initControlsVisibility() {
        fun updateControlsVisibility(isFullScreen: Boolean) {
            areActionsVisible.value = !isFullScreen && areActionsEnabled
            isToolbarVisible.value = !isFullScreen
            isPageIndicatorVisible.value = !isFullScreen && isPageIndicatorEnabled
        }
        isFullScreen.observeForever(::updateControlsVisibility)
    }

    private fun subscribeToRepository() {
        galleryMediaRepository.items
            .doOnNext {
                // Detect emptiness ASAP, otherwise isFresh could be changed
                // by the time the main thread scheduler processes the emission.
                if (it.isEmpty() && galleryMediaRepository.isFresh) {
                    log.debug {
                        "subscribeToRepository(): finishing_as_nothing_left"
                    }

                    eventsSubject.onNext(Event.Finish)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { broadcastItemsFromRepository() }
            .autoDispose(this)

        galleryMediaRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(isLoading::setValue)
            .autoDispose(this)

        galleryMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) { "subscribeToRepository(): error_occurred" }

                val contentLoadingError = GalleryContentLoadingError.from(error)
                eventsSubject.onNext(Event.ShowFloatingError(contentLoadingError))
            }
            .autoDispose(this)
    }

    private fun broadcastItemsFromRepository() {
        if (imageViewSize.width == 0 || imageViewSize.height == 0) {
            log.warn {
                "broadcastItemsFromRepository(): broadcasting_null_without_image_view_size"
            }

            // When the image view size is unknown, new items can't be broadcast
            // while the existing are outdated.
            itemsList.value = null
            return
        }

        itemsList.value = galleryMediaRepository
            .itemsList
            .map { galleryMedia ->
                if (galleryMedia in afterAllNotViewableMedia)
                    MediaViewerPage.unsupported(galleryMedia, previewUrlFactory)
                else
                    MediaViewerPage.fromGalleryMedia(
                        source = galleryMedia,
                        imageViewSize = imageViewSize,
                        livePhotosAsImages = galleryPreferences.livePhotosAsImages.value!!,
                        previewUrlFactory = previewUrlFactory,
                    )
            }
            .also {
                log.debug {
                    "broadcastItemsFromRepository(): broadcasting:" +
                            "\nitemsCount=${it.size}"
                }
            }
    }

    private fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            galleryMediaRepository.update()
        } else {
            galleryMediaRepository.updateIfNotFresh()
        }
    }

    fun loadMore() {
        if (!galleryMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }
            galleryMediaRepository.loadMore()
        }
    }

    fun onShareClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onShareClicked(): position_out_of_range"
            }
            return
        }

        galleryMediaDownloadActionsViewModel.downloadAndShareGalleryMedia(
            media = listOf(item),
        )
    }

    fun onStartSlideshowClicked(position: Int) {
        log.debug {
            "onStartSlideshowClicked(): starting_slideshow:" +
                    "\nposition=$position"
        }

        eventsSubject.onNext(
            Event.OpenSlideshow(
                mediaIndex = position,
                repositoryParams = galleryMediaRepository.params,
            )
        )
    }

    fun onOpenInClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onOpenInClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onOpenInClicked(): start_opening:" +
                    "\nitem=$item"
        }

        galleryMediaDownloadActionsViewModel.downloadAndOpenGalleryMedia(
            media = item,
        )
    }

    fun onOpenInWebViewerClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onOpenInWebViewerClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onOpenInWebViewerClicked(): opening_viewer:" +
                    "\nitem=$item"
        }

        eventsSubject.onNext(
            Event.OpenWebViewer(
                url = webUrlFactory.getWebViewUrl(
                    uid = item.uid,
                )
            )
        )
    }

    fun onAddToAlbumClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onAddToAlbumClicked(): position_out_of_range"
            }
            return
        }

        galleryMediaRemoteActionsViewModel.addGalleryMediaToAlbum(
            mediaUids = setOf(item.uid),
        )
    }

    fun onRemoveFromAlbumClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)
        val albumUid = this.albumUid

        checkNotNull(albumUid) {
            "Removing from the album is only possible when its UID is set"
        }

        if (item == null) {
            log.warn {
                "onRemoveFromAlbumClicked(): position_out_of_range"
            }
            return
        }

        galleryMediaRemoteActionsViewModel.removeGalleryMediaFromAlbum(
            mediaUids = setOf(item.uid),
            albumUid = albumUid,
        )
    }

    fun onArchiveClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onArchiveClicked(): position_out_of_range"
            }
            return
        }

        galleryMediaRemoteActionsViewModel.archiveGalleryMedia(
            mediaUids = listOf(item.uid),
            currentMediaRepository = galleryMediaRepository,
        )
    }

    fun onDeleteClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onDeleteClicked(): position_out_of_range"
            }
            return
        }

        galleryMediaRemoteActionsViewModel.deleteGalleryMedia(
            mediaUids = listOf(item.uid),
            currentMediaRepository = galleryMediaRepository,
        )
    }

    fun onDownloadClicked(position: Int) {
        check(isDownloadButtonVisible.value == true) {
            "The button can't be clicked while it is not visible"
        }

        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onDownloadClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onDownloadClicked(): start_downloading_to_external_storage:" +
                    "\nitem=$item"
        }

        downloadGalleryMediaToExternalStorageInBackground(item)
    }

    fun onCancelDownloadClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onCancelDownloadClicked(): position_out_of_range"
            }
            return
        }

        log.debug {
            "onCancelDownloadClicked(): canceling_download"
        }

        galleryMediaDownloadActionsViewModel.cancelGalleryMediaBackgroundDownload(item.uid)
        unsubscribeFromDownloadProgress()
    }

    fun onFavoriteClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onFavoriteClicked(): position_out_of_range"
            }
            return
        }

        // Switch currently shown favorite state.
        val toSetFavorite = isFavorite.value != true

        galleryMediaRemoteActionsViewModel.updateGalleryMediaAttributes(
            mediaUid = item.uid,
            currentMediaRepository = galleryMediaRepository,
            isFavorite = toSetFavorite,
            onStarted = {
                isFavorite.value = toSetFavorite
            },
        )
    }

    fun onPrivateClicked(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        if (item == null) {
            log.warn {
                "onPrivateClicked(): position_out_of_range"
            }
            return
        }

        // Switch currently shown private state.
        val toSetPrivate = isPrivate.value != true

        galleryMediaRemoteActionsViewModel.updateGalleryMediaAttributes(
            mediaUid = item.uid,
            currentMediaRepository = galleryMediaRepository,
            isPrivate = toSetPrivate,
            onUpdated = {
                isPrivate.value = toSetPrivate
            }
        )
    }

    fun onPageClicked() {
        log.debug { "onPageClicked(): toggling_full_screen" }

        isFullScreen.value = !isFullScreen.value!!
    }

    fun onFullScreenToggledBySystem(isFullScreen: Boolean) {
        if (isFullScreen != this.isFullScreen.value) {
            log.debug {
                "onFullScreenToggledBySystem(): system_toggled_full_screen:" +
                        "\nisFullScreen=$isFullScreen"
            }

            this.isFullScreen.value = isFullScreen
        }
    }

    fun onVideoPlayerFatalPlaybackError(page: MediaViewerPage) {
        val source = page.source

        log.error {
            "onVideoPlayerFatalPlaybackError(): error_occurred:" +
                    "\nsourceMedia=$source"
        }

        if (source != null) {
            afterAllNotViewableMedia.add(source)
            log.debug {
                "onVideoPlayerFatalPlaybackError(): added_media_to_not_viewable:" +
                        "\nmedia=$source"
            }
            broadcastItemsFromRepository()
        }
    }

    private fun downloadGalleryMediaToExternalStorageInBackground(media: GalleryMedia) {
        galleryMediaDownloadActionsViewModel.downloadGalleryMediaToExternalStorageInBackground(
            media = media,
            onDownloadEnqueued = { sendableFile ->
                log.debug {
                    "downloadGalleryMediaToExternalStorageInBackground(): enqueued_download:" +
                            "\nmedia=$media," +
                            "\ndestination=${sendableFile.file}"
                }
                val progressObservable =
                    galleryMediaDownloadActionsViewModel.getGalleryMediaBackgroundDownloadStatus(
                        mediaUid = media.uid,
                    )

                subscribeToMediaBackgroundDownloadStatus(progressObservable)

                eventsSubject.onNext(
                    Event.ShowStartedDownloadMessage(
                        destinationFileName = sendableFile.file.name,
                    )
                )
            }
        )
    }

    fun onPageChanged(position: Int) {
        val item = galleryMediaRepository.itemsList.getOrNull(position)

        log.debug {
            "onPageChanged(): page_changed:" +
                    "\nposition=$position," +
                    "\nitem=$item"
        }

        if (item == null) {
            log.warn {
                "onPageChanged(): position_out_of_range"
            }
            return
        }

        subscribeToMediaBackgroundDownloadStatus(
            statusObservable = galleryMediaDownloadActionsViewModel.getGalleryMediaBackgroundDownloadStatus(
                mediaUid = item.uid,
            )
        )
        updateTitleAndSubtitle(item)
        isFavorite.value = item.isFavorite
        isPrivate.value = item.isPrivate

        // When switching to a video (not live photo or GIF), go full screen if currently is not.
        if (item.media is GalleryMedia.TypeData.Video && isFullScreen.value == false) {
            isFullScreen.value = true
        }
    }

    private var backgroundDownloadProgressDisposable: Disposable? = null
    private fun subscribeToMediaBackgroundDownloadStatus(
        statusObservable: Observable<out BackgroundMediaFileDownloadManager.Status>,
    ) {
        val resetDownloadViews = {
            isCancelDownloadButtonVisible.value = false
            isDownloadCompletedIconVisible.value = false
        }

        backgroundDownloadProgressDisposable?.dispose()
        backgroundDownloadProgressDisposable = statusObservable
            // Here it is important to emit the last item,
            // as the subscription has no separate handler for Observable completion.
            .throttleLatest(500, TimeUnit.MILLISECONDS, true)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { resetDownloadViews() }
            .doOnDispose(resetDownloadViews)
            .subscribeBy(
                onNext = { status ->
                    isCancelDownloadButtonVisible.value =
                        status is BackgroundMediaFileDownloadManager.Status.InProgress
                    isDownloadCompletedIconVisible.value =
                        status is BackgroundMediaFileDownloadManager.Status.Ended.Completed

                    if (status is BackgroundMediaFileDownloadManager.Status.InProgress) {
                        cancelDownloadButtonProgressPercent.value =
                            if (status.percent < 0)
                                -1
                            else
                                status.percent.roundToInt().coerceAtLeast(1)
                    }
                },
            )
            .autoDispose(this)
    }

    private fun unsubscribeFromDownloadProgress() {
        backgroundDownloadProgressDisposable?.dispose()
    }

    private fun updateTitleAndSubtitle(item: GalleryMedia) {
        title.value = item.title

        val takenAtLocal = item.takenAtLocal
        val staticSubtitleValue = staticSubtitle
        subtitle.value =
            if (staticSubtitleValue != null)
                SubtitleValue.Static(staticSubtitleValue)
            else
                SubtitleValue.DateTime(
                    localDate = takenAtLocal,
                    withYear = !takenAtLocal.isSameYearAs(currentLocalDate),
                )
    }

    fun onFloatingErrorRetryClicked() {
        log.debug {
            "onFloatingErrorRetryClicked(): updating"
        }

        update(force = true)
    }

    sealed interface SubtitleValue {
        class Static(val value: String) : SubtitleValue

        class DateTime(
            val localDate: LocalDate,
            val withYear: Boolean,
        ) : SubtitleValue
    }

    sealed interface Event {
        object RequestStoragePermission : Event
        object ShowMissingStoragePermissionMessage : Event
        class ShowStartedDownloadMessage(val destinationFileName: String) : Event
        class OpenWebViewer(val url: String) : Event
        class OpenSlideshow(
            val mediaIndex: Int,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
        ) : Event

        object Finish : Event

        /**
         * Show a dismissible floating error.
         *
         * The [onFloatingErrorRetryClicked] method should be called
         * if the error assumes retrying.
         */
        @JvmInline
        value class ShowFloatingError(
            val error: GalleryContentLoadingError,
        ) : Event
    }
}
