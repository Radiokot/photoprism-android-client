package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.filterIsInstance
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.util.BackPressActionsStack

class GallerySingleRepositoryViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val listViewModel: GalleryListViewModelImpl,
    private val galleryMediaDownloadActionsViewModel: GalleryMediaDownloadActionsViewModelDelegate,
    private val galleryMediaRemoteActionsViewModel: GalleryMediaRemoteActionsViewModelDelegate,
) : ViewModel(),
    GalleryListViewModel by listViewModel,
    GalleryMediaDownloadActionsViewModel by galleryMediaDownloadActionsViewModel,
    GalleryMediaRemoteActionsViewModel by galleryMediaRemoteActionsViewModel {

    private val log = kLogger("GallerySingleRepositoryVM")
    private var isInitialized = false
    private lateinit var currentMediaRepository: SimpleGalleryMediaRepository
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    private val eventsSubject = PublishSubject.create<Event>()
    val events: Observable<Event> = eventsSubject.observeOnMain()
    private val stateSubject = BehaviorSubject.create<State>()
    val state: Observable<State> = stateSubject.observeOnMain()
    val currentState: State
        get() = stateSubject.value!!
    val mainError = MutableLiveData<Error?>(null)
    var canLoadMore = true
        private set
    private var albumUid: String? = null

    private val backPressActionsStack = BackPressActionsStack()
    val backPressedCallback: OnBackPressedCallback =
        backPressActionsStack.onBackPressedCallback
    private val switchBackToViewingOnBackPress = {
        switchToViewing()
    }

    init {
        listViewModel.addDateHeaders = false
    }

    fun initSelectionForAppOnce(
        repositoryParams: SimpleGalleryMediaRepository.Params,
        allowMultiple: Boolean,
    ) {
        if (isInitialized) {
            log.debug {
                "initSelectionForAppOnce(): already_initialized"
            }

            return
        }

        currentMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)

        log.debug {
            "initSelectionForAppOnce(): initialized_selection:" +
                    "\nrepositoryParams=$repositoryParams," +
                    "\nallowMultiple=$allowMultiple"
        }

        stateSubject.onNext(
            State.Selecting.ForOtherApp(
                allowMultiple = allowMultiple,
            )
        )

        if (!allowMultiple) {
            listViewModel.initSelectingSingle(
                onSingleMediaSelected = { media ->
                    galleryMediaDownloadActionsViewModel.downloadAndReturnGalleryMedia(
                        media = listOf(media),
                    )
                },
                shouldPostItemsNow = { true },
            )
        } else {
            listViewModel.initSelectingMultiple(
                shouldPostItemsNow = { true },
            )
        }
        initCommon()

        isInitialized = true
    }

    fun initViewingOnce(
        repositoryParams: SimpleGalleryMediaRepository.Params,
        albumUid: String?,
    ) {
        if (isInitialized) {
            log.debug {
                "initViewingOnce(): already_initialized"
            }

            return
        }

        currentMediaRepository = galleryMediaRepositoryFactory.get(repositoryParams)
        this.albumUid = albumUid

        log.debug {
            "initViewingOnce(): initialized_viewing:" +
                    "\nalbumUid=$albumUid"
        }

        stateSubject.onNext(State.Viewing)

        listViewModel.initViewing(
            onSwitchedFromViewingToSelecting = {
                stateSubject.onNext(
                    State.Selecting.ForUser(
                        canRemoveFromAlbum = albumUid != null,
                    )
                )
                backPressActionsStack.pushUniqueAction(switchBackToViewingOnBackPress)
            },
            onSwitchedFromSelectingToViewing = {
                stateSubject.onNext(State.Viewing)
                backPressActionsStack.removeAction(switchBackToViewingOnBackPress)
            },
            shouldPostItemsNow = { true },
        )
        initCommon()

        isInitialized = true
    }

    private fun initCommon() {
        subscribeToRepository()

        // Replace list VM viewer opening event with the custom one
        // which may contain album UID.
        listViewModel.itemListEvents
            .filterIsInstance<GalleryListViewModel.Event.OpenViewer>()
            .map { originalEvent ->
                Event.OpenViewer(
                    mediaIndex = originalEvent.mediaIndex,
                    repositoryParams = originalEvent.repositoryParams,
                    areActionsEnabled = originalEvent.areActionsEnabled,
                    albumUid = albumUid,
                )
            }
            .subscribe(this, eventsSubject::onNext)

        update()
    }

    private fun subscribeToRepository() {
        log.debug {
            "subscribeToRepository(): subscribing:" +
                    "\nrepository=$currentMediaRepository"
        }

        currentMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { items ->
                mainError.value = when {
                    items.isEmpty() && !currentMediaRepository.isNeverUpdated ->
                        Error.NoMediaFound

                    else ->
                        null
                }

                postGalleryItemsAsync(currentMediaRepository)
            }
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

                val contentLoadingError = Error.ContentLoadingError(
                    GalleryContentLoadingError.from(error)
                )

                if (itemList.value.isNullOrEmpty()) {
                    mainError.value = contentLoadingError
                } else {
                    eventsSubject.onNext(Event.ShowFloatingError(contentLoadingError))
                }
            }
            .autoDispose(this)
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            currentMediaRepository.updateIfNotFresh()
        } else {
            currentMediaRepository.update()
            eventsSubject.onNext(Event.ResetScroll)
        }
    }

    fun loadMore() {
        if (!currentMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }

            currentMediaRepository.loadMore()
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

    fun onDoneMultipleSelectionClicked() {
        val currentState = this.currentState
        check(currentState is State.Selecting.ForOtherApp && currentState.allowMultiple) {
            "Done multiple selection button is only clickable when selecting multiple for other app"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Done multiple selection button is only clickable when something is selected"
        }

        galleryMediaDownloadActionsViewModel.downloadAndReturnGalleryMedia(
            media = selectedMediaByUid.values,
        )
    }

    fun onShareMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Share multiple selection button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Share multiple selection button is only clickable when something is selected"
        }

        galleryMediaDownloadActionsViewModel.downloadAndShareGalleryMedia(
            media = selectedMediaByUid.values,
            onShared = {
                switchToViewing()
            }
        )
    }

    fun onDownloadMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Download multiple selection button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Download multiple selection button is only clickable when something is selected"
        }

        galleryMediaDownloadActionsViewModel.downloadGalleryMediaToExternalStorage(
            media = selectedMediaByUid.values,
            onDownloadFinished = {
                switchToViewing()
            }
        )
    }

    fun onAddToAlbumMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Adding multiple selection to album button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Adding multiple selection to album button is only clickable when something is selected"
        }

        galleryMediaRemoteActionsViewModel.addGalleryMediaToAlbum(
            mediaUids = selectedMediaByUid.keys.toList(),
            onStarted = ::switchToViewing,
        )
    }

    fun onRemoveFromAlbumMultipleSelectionClicked() {
        val currentState = this.currentState
        val albumUid = this.albumUid

        check(currentState is State.Selecting.ForUser && currentState.canRemoveFromAlbum) {
            "Removing multiple selection from album button is only clickable when selecting " +
                    "and it is allowed"
        }

        checkNotNull(albumUid) {
            "Removing multiple selection from album is only possible when there is the album UID"
        }

        galleryMediaRemoteActionsViewModel.removeGalleryMediaFromAlbum(
            mediaUids = selectedMediaByUid.keys.toList(),
            albumUid = albumUid,
            onStarted = ::switchToViewing,
        )
    }

    fun onArchiveMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Archive multiple selection button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Archive multiple selection button is only clickable when something is selected"
        }

        galleryMediaRemoteActionsViewModel.archiveGalleryMedia(
            mediaUids = selectedMediaByUid.keys.toList(),
            currentMediaRepository = currentMediaRepository,
            onStarted = ::switchToViewing,
        )
    }

    fun onDeleteMultipleSelectionClicked() {
        check(currentState is State.Selecting.ForUser) {
            "Delete multiple selection button is only clickable when selecting"
        }

        check(selectedMediaByUid.isNotEmpty()) {
            "Delete multiple selection button is only clickable when something is selected"
        }

        galleryMediaRemoteActionsViewModel.deleteGalleryMedia(
            mediaUids = selectedMediaByUid.keys.toList(),
            currentMediaRepository = currentMediaRepository,
            onStarted = ::switchToViewing,
        )
    }

    private fun switchToViewing() {
        assert(currentState is State.Selecting.ForUser) {
            "Switching to viewing is only possible while selecting to share"
        }

        listViewModel.switchFromSelectingToViewing()
    }

    fun onSwipeRefreshPulled() {
        log.debug {
            "onSwipeRefreshPulled(): force_updating"
        }

        update(force = true)
    }

    sealed interface State {
        /**
         * Viewing the gallery content.
         */
        object Viewing : State

        /**
         * Viewing the gallery content to select something.
         */
        sealed class Selecting(
            /**
             * Whether selection of multiple items is allowed or not.
             */
            val allowMultiple: Boolean,
        ) : State {

            /**
             * Selecting to return the files to the requesting app.
             */
            class ForOtherApp(
                allowMultiple: Boolean,
            ) : Selecting(
                allowMultiple = allowMultiple,
            )

            /**
             * Selecting to share the files with any app of the user's choice.
             */
            class ForUser(
                val canRemoveFromAlbum: Boolean,
            ) : Selecting(
                allowMultiple = true,
            )
        }
    }

    sealed interface Event {
        /**
         * Reset the scroll (to the top) and the infinite scrolling.
         */
        object ResetScroll : Event

        /**
         * Show a dismissible floating error.
         *
         * The [onFloatingErrorRetryClicked] method should be called
         * if the error assumes retrying.
         */
        @JvmInline
        value class ShowFloatingError(val error: Error) : Event

        class OpenViewer(
            val mediaIndex: Int,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
            val areActionsEnabled: Boolean,
            val albumUid: String?,
        ) : Event
    }

    sealed interface Error {
        @JvmInline
        value class ContentLoadingError(
            val contentLoadingError: GalleryContentLoadingError,
        ) : Error

        /**
         * Nothing is found for the given search.
         * The gallery may be empty as well.
         */
        object NoMediaFound : Error
    }
}
