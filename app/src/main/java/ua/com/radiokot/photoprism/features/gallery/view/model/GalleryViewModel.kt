package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import java.io.File
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.DateFormat
import java.util.*

class GalleryViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
    private val dateHeaderDayYearDateFormat: DateFormat,
    private val dateHeaderDayDateFormat: DateFormat,
) : ViewModel() {
    private val log = kLogger("GalleryVM")
    private lateinit var initialMediaRepository: SimpleGalleryMediaRepository
    private lateinit var currentMediaRepository: SimpleGalleryMediaRepository
    private var isInitialized = false

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<GalleryListItem>?> = MutableLiveData(null)
    private val eventsSubject: PublishSubject<Event> = PublishSubject.create()
    val events: Observable<Event> = eventsSubject
    val state: MutableLiveData<State> = MutableLiveData()
    val mainError = MutableLiveData<Error?>(null)
    var canLoadMore = true
        private set

    private lateinit var downloadMediaFileViewModel: DownloadMediaFileViewModel
    private lateinit var searchViewModel: GallerySearchViewModel

    fun initSelectionOnce(
        downloadViewModel: DownloadMediaFileViewModel,
        searchViewModel: GallerySearchViewModel,
        requestedMimeType: String?,
    ) {
        if (isInitialized) {
            log.debug {
                "initSelection(): already_initialized"
            }

            return
        }

        val filterMediaTypes: Set<GalleryMedia.TypeName> = when {
            requestedMimeType == null ->
                emptySet()
            requestedMimeType.startsWith("image/") ->
                setOf(
                    GalleryMedia.TypeName.IMAGE,
                    GalleryMedia.TypeName.ANIMATED,
                    GalleryMedia.TypeName.VECTOR
                )
            requestedMimeType.startsWith("video/") ->
                setOf(
                    GalleryMedia.TypeName.VIDEO,
                    GalleryMedia.TypeName.LIVE,
                )
            else ->
                emptySet()
        }

        downloadMediaFileViewModel = downloadViewModel
        this.searchViewModel = searchViewModel

        if (filterMediaTypes.isNotEmpty()) {
            searchViewModel.availableMediaTypes.value = filterMediaTypes
        }

        initialMediaRepository = galleryMediaRepositoryFactory.getFiltered(filterMediaTypes)
        currentMediaRepository = initialMediaRepository

        log.debug {
            "initSelection(): initialized_selection:" +
                    "\nrequestedMimeType=$requestedMimeType," +
                    "\nmatchedFilterMediaTypes=$filterMediaTypes"
        }

        state.value = State.Selecting

        subscribeToSearch()

        isInitialized = true
    }

    fun initViewingOnce(
        downloadViewModel: DownloadMediaFileViewModel,
        searchViewModel: GallerySearchViewModel,
    ) {
        if (isInitialized) {
            log.debug {
                "initViewing(): already_initialized"
            }

            return
        }

        downloadMediaFileViewModel = downloadViewModel
        this.searchViewModel = searchViewModel

        initialMediaRepository = galleryMediaRepositoryFactory.getFiltered()
        currentMediaRepository = initialMediaRepository

        log.debug {
            "initViewing(): initialized_viewing"
        }

        state.value = State.Viewing

        subscribeToSearch()

        isInitialized = true
    }

    private fun subscribeToSearch() {
        searchViewModel.state.subscribe { state ->
            when (state) {
                is GallerySearchViewModel.State.AppliedSearch -> {
                    currentMediaRepository = galleryMediaRepositoryFactory.getFiltered(
                        mediaTypes = state.search.mediaTypes,
                        userQuery = state.search.userQuery,
                    )
                    subscribeToRepository()
                    update()
                }
                GallerySearchViewModel.State.NoSearch -> {
                    // TODO: initial repository saved here but removed from factory cache ->
                    //  photo viewer is opened with a fresh repository.
                    currentMediaRepository = initialMediaRepository
                    subscribeToRepository()
                    update()
                }
                is GallerySearchViewModel.State.ConfiguringSearch -> {
                }
            }
        }
            .addToCloseables(this)
    }

    private var repositorySubscriptionDisposable: CompositeDisposable? = null
    private fun subscribeToRepository() {
        repositorySubscriptionDisposable?.dispose()

        val disposable = CompositeDisposable()
        repositorySubscriptionDisposable = disposable

        log.debug {
            "subscribeToRepository(): subscribing:" +
                    "\nrepository=$currentMediaRepository"
        }

        currentMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::onNewRepositoryItems)
            .addTo(disposable)

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
            .addTo(disposable)

        currentMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) { "subscribeToRepository(): error_occurred" }

                val viewError = when (error) {
                    is UnknownHostException,
                    is NoRouteToHostException,
                    is SocketTimeoutException ->
                        Error.LibraryNotAccessible
                    else ->
                        Error.LoadingFailed
                }

                if (itemsList.value.isNullOrEmpty()) {
                    mainError.value = viewError
                } else {
                    eventsSubject.onNext(Event.ShowFloatingError(viewError))
                }
            }
            .addTo(disposable)

        disposable.addToCloseables(this)

        eventsSubject.onNext(Event.ResetScroll)
    }

    private fun onNewRepositoryItems(galleryMediaItems: List<GalleryMedia>) {
        // Dismiss the main error when there are items.
        mainError.value = null

        val newListItems = mutableListOf<GalleryListItem>()

        // Add date headers.
        val today = Date()
        galleryMediaItems
            .forEachIndexed { i, galleryMedia ->
                val takenAt = galleryMedia.takenAt

                if (i == 0 || !takenAt.isSameDayAs(galleryMediaItems[i - 1].takenAt)) {
                    newListItems.add(
                        if (takenAt.isSameDayAs(today))
                            GalleryListItem.Header(
                                textRes = R.string.today,
                                identifier = takenAt.time,
                            )
                        else {
                            val formattedDate =
                                if (takenAt.isSameYearAs(today))
                                    dateHeaderDayDateFormat.format(takenAt)
                                else
                                    dateHeaderDayYearDateFormat.format(takenAt)

                            GalleryListItem.Header(
                                text = formattedDate.replaceFirstChar {
                                    if (it.isLowerCase())
                                        it.titlecase(Locale.getDefault())
                                    else
                                        it.toString()
                                },
                                identifier = takenAt.time,
                            )
                        }
                    )
                }

                newListItems.add(
                    GalleryListItem.Media(
                        source = galleryMedia,
                    )
                )
            }

        itemsList.value = newListItems
    }

    private fun Date.isSameDayAs(other: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = this
        val thisYear = calendar[Calendar.YEAR]
        val thisDay = calendar[Calendar.DAY_OF_YEAR]
        calendar.time = other
        val otherYear = calendar[Calendar.YEAR]
        val otherDay = calendar[Calendar.DAY_OF_YEAR]

        return thisYear == otherYear && thisDay == otherDay
    }

    private fun Date.isSameYearAs(other: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = this
        val thisYear = calendar[Calendar.YEAR]
        calendar.time = other
        val otherYear = calendar[Calendar.YEAR]

        return thisYear == otherYear
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            currentMediaRepository.updateIfNotFresh()
        } else {
            currentMediaRepository.update()
        }
    }

    fun loadMore() {
        if (!currentMediaRepository.isLoading) {
            log.debug { "loadMore(): requesting_load_more" }
            currentMediaRepository.loadMore()
        }
    }

    fun onItemClicked(item: GalleryListItem) {
        log.debug {
            "onItemClicked(): gallery_item_clicked:" +
                    "\nitem=$item"
        }

        if (item !is GalleryListItem.Media) {
            return
        }

        when (state.value.checkNotNull()) {
            is State.Selecting -> {
                if (item.source != null) {
                    if (item.source.files.size > 1) {
                        openFileSelectionDialog(item.source.files)
                    } else {
                        downloadAndReturnFile(item.source.files.firstOrNull().checkNotNull {
                            "There must be at least one file in the gallery media object"
                        })
                    }
                }
            }

            is State.Viewing ->
                if (item.source != null) {
                    openViewer(item.source)
                }
        }
    }

    private fun openFileSelectionDialog(files: List<GalleryMedia.File>) {
        log.debug {
            "openFileSelectionDialog(): posting_open_event:" +
                    "\nfiles=$files"
        }

        eventsSubject.onNext(Event.OpenFileSelectionDialog(files))
    }

    fun onFileSelected(file: GalleryMedia.File) {
        log.debug {
            "onFileSelected(): file_selected:" +
                    "\nfile=$file"
        }

        downloadAndReturnFile(file)
    }

    private fun downloadAndReturnFile(file: GalleryMedia.File) {
        downloadMediaFileViewModel.downloadFile(
            file = file,
            onSuccess = { destinationFile ->
                eventsSubject.onNext(
                    Event.ReturnDownloadedFile(
                        downloadedFile = destinationFile,
                        mimeType = file.mimeType,
                        displayName = File(file.name).name
                    )
                )
            }
        )
    }

    private fun openViewer(media: GalleryMedia) {
        val index = currentMediaRepository.itemsList.indexOf(media)
        val repositoryQuery = currentMediaRepository.query

        log.debug {
            "openViewer(): opening_viewer:" +
                    "\nmedia=$media," +
                    "\nindex=$index," +
                    "\nrepositoryQuery=$repositoryQuery"
        }

        eventsSubject.onNext(
            Event.OpenViewer(
                mediaIndex = index,
                repositoryQuery = repositoryQuery,
            )
        )
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

    sealed interface Event {
        class OpenFileSelectionDialog(val files: List<GalleryMedia.File>) : Event

        class ReturnDownloadedFile(
            val downloadedFile: File,
            val mimeType: String,
            val displayName: String,
        ) : Event

        class OpenViewer(
            val mediaIndex: Int,
            val repositoryQuery: String?,
        ) : Event

        object ResetScroll : Event

        class ShowFloatingError(val error: Error) : Event
    }

    sealed interface State {
        object Viewing : State
        object Selecting : State
    }

    sealed interface Error {
        object LibraryNotAccessible : Error
        object LoadingFailed : Error
    }
}