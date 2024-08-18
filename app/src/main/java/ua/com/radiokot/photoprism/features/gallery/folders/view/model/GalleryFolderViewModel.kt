package ua.com.radiokot.photoprism.features.gallery.folders.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel.Error
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel.Event
import java.util.concurrent.TimeUnit

class GalleryFolderViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val galleryPreferences: GalleryPreferences,
) : ViewModel() {

    private val log = kLogger("GalleryFolderVM")
    private lateinit var currentMediaRepository: SimpleGalleryMediaRepository
    private var isInitialized = false
    private val galleryItemsPostingSubject = PublishSubject.create<SimpleGalleryMediaRepository>()
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()
    val isLoading = MutableLiveData(false)
    val itemsList: MutableLiveData<List<GalleryListItem>?> = MutableLiveData(null)
    val mainError = MutableLiveData<Error?>(null)
    var canLoadMore = true
        private set
    val itemScale: GalleryItemScale
        get() = galleryPreferences.itemScale.value.checkNotNull {
            "There must be an item scale to consider"
        }

    fun initViewingOnce(
        repositoryParams: SimpleGalleryMediaRepository.Params,
    ) {
        if (isInitialized) {
            return
        }

        currentMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)

        initCommon()

        log.debug {
            "initViewingOnce(): initialized:" +
                    "\nrepositoryParams=$repositoryParams"
        }

        isInitialized = true
    }

    private fun initCommon() {
        subscribeGalleryItemsPosting()
        subscribeToRepository()

        update()
    }

    private fun subscribeToRepository() {
        currentMediaRepository.items
            .subscribe { postGalleryItemsAsync() }
            .autoDispose(this)

        currentMediaRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { isLoading ->
                canLoadMore = !currentMediaRepository.noMoreItems
                this.isLoading.value = isLoading

                // Dismiss the main error when something is loading.
                if (isLoading) {
                    mainError.value = null
                }
            }
            .autoDispose(this)

        currentMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) { "subscribeToRepository(): error_occurred" }

                if (itemsList.value.isNullOrEmpty()) {
                    mainError.value = Error.LoadingFailed(error.shortSummary)
                } else {
                    eventsSubject.onNext(Event.ShowFloatingLoadingFailedError)
                }
            }
            .autoDispose(this)
    }

    private fun subscribeGalleryItemsPosting() =
        galleryItemsPostingSubject
            .observeOn(Schedulers.computation())
            // Post empty lists immediately for better visual,
            // therefore do not proceed further.
            .filter { repository ->
                if (repository.itemsList.isEmpty()) {
                    postGalleryItems(repository)
                    false
                } else {
                    true
                }
            }
            // Small debounce is nice for situations when multiple changes
            // trigger items posting, e.g. state and repository.
            .debounce(30, TimeUnit.MILLISECONDS)
            // Proceed only if the repository remained the same.
            .filter { it == currentMediaRepository }
            .subscribe(::postGalleryItems)
            .autoDispose(this)

    /**
     * Schedules preparation and posting the items in a non-main thread.
     *
     * @see subscribeGalleryItemsPosting
     */
    private fun postGalleryItemsAsync() {
        val repository = currentMediaRepository.checkNotNull {
            "There must be a media repository to post items from"
        }
        galleryItemsPostingSubject.onNext(repository)
    }

    private fun postGalleryItems(repository: SimpleGalleryMediaRepository) {
        val galleryMediaList = repository.itemsList

        mainError.postValue(
            when {
                galleryMediaList.isEmpty() && !repository.isNeverUpdated ->
                    Error.NoMediaFound

                else ->
                    // Dismiss the main error when there are items.
                    null
            }
        )

        val newListItems = galleryMediaList.map { galleryMedia ->
            GalleryListItem.Media(
                source = galleryMedia,
                isViewButtonVisible = false,
                isSelectionViewVisible = false,
                isMediaSelected = false,
                itemScale = itemScale,
            )
        }

        itemsList.postValue(newListItems)
    }

    private fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            currentMediaRepository.update()
        } else {
            currentMediaRepository.updateIfNotFresh()
        }
    }

    fun onMainErrorRetryClicked() {
        update()
    }

    fun onFloatingErrorRetryClicked() {
        loadMore()
    }

    fun onLoadingFooterLoadMoreClicked() {
        loadMore()
    }

    fun loadMore() {
        if (!currentMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }

            currentMediaRepository.loadMore()
        }
    }

    fun onSwipeRefreshPulled() {
        log.debug {
            "onSwipeRefreshPulled(): force_updating"
        }

        update(force = true)
    }

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         * Retry is possible: the [onFloatingErrorRetryClicked] method should be called.
         */
        object ShowFloatingLoadingFailedError : Event
    }

    sealed interface Error {
        /**
         * The data has been requested, but something went wrong
         * while receiving the response.
         */
        class LoadingFailed(val shortSummary: String) : Error

        /**
         * Nothing is found for the given search.
         * The gallery may be empty as well.
         */
        object NoMediaFound : Error
    }
}
