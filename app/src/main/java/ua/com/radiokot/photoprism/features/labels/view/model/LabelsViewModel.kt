package ua.com.radiokot.photoprism.features.labels.view.model

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.labels.data.model.Label
import ua.com.radiokot.photoprism.features.labels.data.storage.LabelsRepository
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModel
import ua.com.radiokot.photoprism.features.shared.search.view.model.SearchViewViewModelImpl

class LabelsViewModel(
    private val labelsRepositoryFactory: LabelsRepository.Factory,
    private val searchPredicate: (label: Label, query: String) -> Boolean,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) : ViewModel(),
    SearchViewViewModel by SearchViewViewModelImpl() {

    private val log = kLogger("LabelsVM")
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<LabelListItem>>()
    val mainError = MutableLiveData<Error?>(null)
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPressed()
    }
    private var isInitialized = false
    private lateinit var defaultSearchConfig: SearchConfig
    private val repositoryChanges: BehaviorSubject<LabelsRepository> = BehaviorSubject.create()
    private val labelsRepository: LabelsRepository
        get() = repositoryChanges.value!!

    fun initOnce(
        defaultSearchConfig: SearchConfig,
    ) {
        if (isInitialized) {
            return
        }

        this.defaultSearchConfig = defaultSearchConfig

        repositoryChanges.onNext(
            labelsRepositoryFactory.get(
                isAllLabels = false,
            )
        )

        subscribeToRepositoryChanges()
        subscribeToSearch()

        update()

        isInitialized = true

        log.debug {
            "initOnce(): initialized"
        }
    }

    private fun subscribeToRepositoryChanges() {
        repositoryChanges
            .distinctUntilChanged()
            .subscribe {
                subscribeToRepository()
                update()
            }
            .autoDispose(this)
    }

    private var repositorySubscriptionDisposable: CompositeDisposable? = null
    private fun subscribeToRepository() {
        repositorySubscriptionDisposable?.dispose()

        val disposable = CompositeDisposable()
        repositorySubscriptionDisposable = disposable

        log.debug {
            "subscribeToRepository(): subscribing:" +
                    "\nrepository=$labelsRepository"
        }

        labelsRepository.items
            .filter { !labelsRepository.isNeverUpdated }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postLabelItems() }
            .autoDispose(this)

        labelsRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepository(): labels_loading_failed"
                }

                if (itemsList.value == null) {
                    mainError.value = Error.LoadingFailed
                } else {
                    eventsSubject.onNext(Event.ShowFloatingLoadingFailedError)
                }
            }
            .autoDispose(this)

        labelsRepository.loading
            .subscribe(isLoading::postValue)
            .autoDispose(this)
    }

    private fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            labelsRepository.update()
        } else {
            labelsRepository.updateIfNotFresh()
        }
    }

    private fun subscribeToSearch() {
        searchInputObservable
            .filter { itemsList.value != null }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postLabelItems() }
            .autoDispose(this)
    }

    private fun postLabelItems() {
        val repositoryLabels = labelsRepository.itemsList
        val searchQuery = currentSearchInput
        val filteredRepositoryLabels =
            if (searchQuery != null)
                repositoryLabels.filter { label ->
                    searchPredicate(label, searchQuery)
                }
            else
                repositoryLabels

        log.debug {
            "postLabelItems(): posting_items:" +
                    "\nlabelCount=${repositoryLabels.size}," +
                    "\nsearchQuery=$searchQuery," +
                    "\nfilteredLabelCount=${filteredRepositoryLabels.size}"
        }

        itemsList.value = filteredRepositoryLabels
            .map { label ->
                LabelListItem(
                    source = label,
                    previewUrlFactory = previewUrlFactory,
                )
            }

        mainError.value =
            when {
                filteredRepositoryLabels.isEmpty() ->
                    Error.NothingFound

                else ->
                    // Dismiss the main error when there are items.
                    null
            }
    }

    fun onRetryClicked() {
        log.debug {
            "onRetryClicked(): updating"
        }

        update(force = true)
    }

    fun onSwipeRefreshPulled() {
        log.debug {
            "onSwipeRefreshPulled(): force_updating"
        }

        update(force = true)
    }

    fun onLabelItemClicked(item: LabelListItem) {
        log.debug {
            "onLabelItemClicked(): label_item_clicked:" +
                    "\nitem=$item"
        }

        val label = item.source
            ?: return

        log.debug {
            "onLabelItemClicked(): opening_label:" +
                    "\nslug=${label.slug}"
        }

        eventsSubject.onNext(
            Event.OpenLabel(
                name = label.name,
                repositoryParams = SimpleGalleryMediaRepository.Params(
                    searchConfig = SearchConfig.forLabel(
                        labelSlug = label.slug,
                        base = defaultSearchConfig,
                    )
                )
            )
        )
    }

    private fun onBackPressed() {
        log.debug {
            "onBackPressed(): handling_back_press"
        }

        when {
            isSearchExpanded.value == true -> {
                closeAndClearSearch()
            }

            else -> {
                log.debug {
                    "onBackPressed(): finishing_without_result"
                }

                eventsSubject.onNext(Event.Finish)
            }
        }
    }

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         * Retry is possible: the [onRetryClicked] method should be called.
         */
        object ShowFloatingLoadingFailedError : Event

        object Finish : Event

        class OpenLabel(
            val name: String,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
        ) : Event
    }

    sealed interface Error {
        /**
         * The loading is failed and could be retried.
         * The [onRetryClicked] method should be called.
         */
        object LoadingFailed : Error

        /**
         * No folders found for the filter or there are simply no folders.
         */
        object NothingFound : Error
    }
}
