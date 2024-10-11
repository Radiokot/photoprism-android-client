package ua.com.radiokot.photoprism.features.viewer.slideshow.view.model

import android.util.Size
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
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
import ua.com.radiokot.photoprism.features.viewer.slideshow.data.storage.SlideshowPreferences
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoViewerPage
import java.util.concurrent.TimeUnit

class SlideshowViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val slideshowPreferences: SlideshowPreferences,
    private val galleryPreferences: GalleryPreferences,
) : ViewModel() {
    private val log = kLogger("SlideshowVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository
    private var isInitialized = false

    // Media that turned to be not viewable.
    private val afterAllNotViewableMedia = mutableSetOf<GalleryMedia>()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPage>?> = MutableLiveData(null)
    val currentPageIndex: MutableLiveData<Int> = MutableLiveData(0)

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

    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()

    fun initOnce(
        startPageIndex: Int,
        repositoryParams: SimpleGalleryMediaRepository.Params,
    ) {
        if (isInitialized) {
            return
        }

        galleryMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)
            .checkNotNull {
                "The repository must be created beforehand"
            }
        subscribeToRepository()

        currentPageIndex.value = startPageIndex

        if (!slideshowPreferences.isGuideAccepted) {
            log.debug {
                "initOnce(): opening_guide"
            }

            eventsSubject.onNext(Event.OpenGuide)
        }

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\nstartPageIndex=$startPageIndex" +
                    "\nrepositoryParam=$repositoryParams"
        }

        update()
    }

    private fun subscribeToRepository() {
        galleryMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { broadcastItemsFromRepository() }
            .autoDispose(this)

        galleryMediaRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(isLoading::setValue)
            .autoDispose(this)

        galleryMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                log.error(it) { "subscribeToRepository(): error_occurred" }
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
                    MediaViewerPage.unsupported(galleryMedia)
                else
                    MediaViewerPage.fromGalleryMedia(
                        source = galleryMedia,
                        imageViewSize = imageViewSize,
                        livePhotosAsImages = galleryPreferences.livePhotosAsImages.value!!,
                    )
            }
            .also {
                log.debug {
                    "broadcastItemsFromRepository(): broadcasting:" +
                            "\nitemsCount=${it.size}"
                }
            }
    }

    private fun update() {
        galleryMediaRepository.updateIfNotFresh()
    }

    fun loadMore() {
        if (!galleryMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }
            galleryMediaRepository.loadMore()
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

    fun onPageContentPresented(pageIndex: Int) {
        val currentPageIndex = currentPageIndex.value
            ?: return
        val currentPage: MediaViewerPage = itemsList.value?.getOrNull(currentPageIndex)
            ?: return
        val lastPageIndex = itemsList.value?.size?.dec()
            ?: return
        val isLastPage = currentPageIndex == lastPageIndex
        val nextPageIndex =
            // Switch to the next page unless restarting at the end.
            if (!isLastPage)
                currentPageIndex + 1
            else
                0

        log.debug {
            "onPageContentPresented(): content_presented:" +
                    "\npageIndex=$pageIndex," +
                    "\ncurrentPageIndex=$currentPageIndex," +
                    "\nlastPageIndex=$lastPageIndex," +
                    "\ncurrentPage=$currentPage," +
                    "\nnextPageIndex=$nextPageIndex"
        }

        // The 'page content presented' callback may be launched late,
        // we only care if the current page has been presented.
        if (pageIndex == currentPageIndex) {
            when (currentPage) {
                is VideoViewerPage ->
                    // For videos, switch immediately after playback end.
                    scheduleSwitchingPage(
                        destinationPageIndex = nextPageIndex,
                        delayMs = 0,
                    )

                else ->
                    scheduleSwitchingPage(
                        destinationPageIndex = nextPageIndex,
                        delayMs = slideshowPreferences.speed.presentationDurationMs,
                    )
            }
        }
    }

    fun onStartAreaClicked() {
        val currentPageIndex = currentPageIndex.value
            ?: return

        // Switch to the previous page if the current is not the first one.
        if (currentPageIndex != 0) {
            scheduleSwitchingPage(
                destinationPageIndex = currentPageIndex - 1,
                delayMs = 0,
            )
        }
    }

    fun onEndAreaClicked() {
        val currentPageIndex = currentPageIndex.value
            ?: return
        val lastPageIndex = itemsList.value?.size?.dec()
            ?: return

        // Switch to the next page if the current is not the last one.
        if (currentPageIndex < lastPageIndex) {
            scheduleSwitchingPage(
                destinationPageIndex = currentPageIndex + 1,
                delayMs = 0,
            )
        }
    }

    private var switchingPageDisposable: Disposable? = null
    private fun scheduleSwitchingPage(
        destinationPageIndex: Int,
        delayMs: Int,
    ) {
        log.debug {
            "scheduleSwitchingPage(): scheduling:" +
                    "\ndelayMs=$delayMs" +
                    "\ndestinationPageIndex=$destinationPageIndex"
        }

        switchingPageDisposable?.dispose()
        switchingPageDisposable = Single.timer(delayMs.toLong(), TimeUnit.MILLISECONDS)
            .subscribeBy {
                currentPageIndex.postValue(destinationPageIndex)
            }
            .autoDispose(this)
    }

    sealed interface Event {
        object OpenGuide : Event
    }
}
