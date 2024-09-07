package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListViewModel.Event
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListViewModel.State
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.concurrent.TimeUnit

class GalleryListViewModelImpl(
    galleryPreferences: GalleryPreferences,
) : ViewModel(),
    GalleryListViewModel {

    private val log = kLogger("GalleryListViewModelImpl")

    override val itemListState: BehaviorSubject<State> =
        BehaviorSubject.create()
    override val itemListEvents: PublishSubject<Event> =
        PublishSubject.create()
    override val itemList: BehaviorSubject<List<GalleryListItem>> =
        BehaviorSubject.create()
    override val itemScale: BehaviorSubject<GalleryItemScale> =
        galleryPreferences.itemScale
    override val selectedItemsCount: BehaviorSubject<Int> =
        BehaviorSubject.createDefault(0)
    override val selectedFilesByMediaUid =
        linkedMapOf<String, GalleryMedia.File>()

    private val currentState: State
        get() = itemListState.value!!
    private val currentLocalDate = LocalDate()
    private val itemPostingSubject: PublishSubject<SimpleGalleryMediaRepository> =
        PublishSubject.create()
    private lateinit var currentMediaRepository: SimpleGalleryMediaRepository
    private var onSingleMediaFileSelected: ((GalleryMedia.File) -> Unit)? = null
    private var onSwitchedFromViewingToSelecting: (() -> Unit)? = null
    private var onSwitchedFromSelectingToViewing: (() -> Unit)? = null
    private var canSwitchFromSelectingToViewing = true
    var addDateHeaders = true

    fun initSelectingSingle(
        onSingleMediaFileSelected: (GalleryMedia.File) -> Unit,
        shouldPostItemsNow: (SimpleGalleryMediaRepository) -> Boolean,
    ) {
        this.onSingleMediaFileSelected = onSingleMediaFileSelected
        canSwitchFromSelectingToViewing = false

        initAsyncItemPosting(shouldPostItemsNow)
        initScaleChange()

        itemListState.onNext(
            State.Selecting(
                allowMultiple = false,
            )
        )
    }

    fun initSelectingMultiple(
        shouldPostItemsNow: (SimpleGalleryMediaRepository) -> Boolean,
    ) {
        canSwitchFromSelectingToViewing = false

        initAsyncItemPosting(shouldPostItemsNow)
        initScaleChange()

        itemListState.onNext(
            State.Selecting(
                allowMultiple = true,
            )
        )
    }

    fun initViewing(
        onSwitchedFromViewingToSelecting: () -> Unit,
        onSwitchedFromSelectingToViewing: () -> Unit,
        shouldPostItemsNow: (SimpleGalleryMediaRepository) -> Boolean,
    ) {
        this.onSwitchedFromViewingToSelecting = onSwitchedFromViewingToSelecting
        this.onSwitchedFromSelectingToViewing = onSwitchedFromSelectingToViewing
        canSwitchFromSelectingToViewing = true

        initAsyncItemPosting(shouldPostItemsNow)
        initScaleChange()

        itemListState.onNext(State.Viewing)
    }

    private fun initAsyncItemPosting(
        shouldPostNow: (SimpleGalleryMediaRepository) -> Boolean,
    ) =
        itemPostingSubject
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
            .filter { shouldPostNow(it) }
            .subscribe(::postGalleryItems)
            .autoDispose(this)

    private fun initScaleChange() =
        itemScale
            .toMainThreadObservable()
            .skip(1)
            .distinctUntilChanged()
            .subscribe { newItemScale ->
                val postItems = itemList.value != null && ::currentMediaRepository.isInitialized

                log.debug {
                    "initScaleChange(): item_scale_changed:" +
                            "\nnewItemScale=$newItemScale," +
                            "\npostItems=$postItems"
                }

                if (postItems) {
                    postGalleryItemsAsync(currentMediaRepository)
                }
            }
            .autoDispose(this)

    override fun postGalleryItemsAsync(repository: SimpleGalleryMediaRepository) {
        itemPostingSubject.onNext(repository)
    }

    override fun postGalleryItems(repository: SimpleGalleryMediaRepository) {
        currentMediaRepository = repository

        val galleryMediaList = repository.itemsList
        val itemScale = itemScale.value!!

        val currentState = this.currentState
        val areViewButtonsVisible =
            currentState is State.Selecting
        val areSelectionViewsVisible =
            currentState is State.Selecting && currentState.allowMultiple
        val onlyGroupByMonths = itemScale == GalleryItemScale.TINY

        val newItemList = buildList(galleryMediaList.size) {
            galleryMediaList.forEachIndexed { i, galleryMedia ->
                val takenAtLocal = galleryMedia.takenAtLocal

                if (addDateHeaders) {
                    // Month header.
                    //
                    // For the first item – show if only grouping by months, or if its month
                    // doesn't match the current (e.g. it is November, but the first photo is from October).
                    //
                    // For other items – show on month change, that is when the item's month
                    // doesn't match the previous one's.
                    if (i == 0 && (onlyGroupByMonths || !takenAtLocal.isSameMonthAs(currentLocalDate))
                        || i != 0 && !takenAtLocal.isSameMonthAs(galleryMediaList[i - 1].takenAtLocal)
                    ) {
                        add(
                            GalleryListItem.Header.month(
                                localDate = takenAtLocal,
                                withYear = !takenAtLocal.isSameYearAs(currentLocalDate),
                            )
                        )
                    }

                    // Day header.
                    //
                    // Do not show if only grouping by months.
                    //
                    // For the first item – always show.
                    //
                    // For other items – show on day change, that is when the item's day
                    // doesn't match the previous one's.
                    if (!onlyGroupByMonths
                        && (i == 0 || !takenAtLocal.isSameDayAs(galleryMediaList[i - 1].takenAtLocal))
                    ) {
                        add(
                            if (takenAtLocal.isSameDayAs(currentLocalDate))
                                GalleryListItem.Header.today()
                            else
                                GalleryListItem.Header.day(
                                    localDate = takenAtLocal,
                                    withYear = !takenAtLocal.isSameYearAs(currentLocalDate),
                                )
                        )
                    }
                }

                add(
                    GalleryListItem.Media(
                        source = galleryMedia,
                        isViewButtonVisible = areViewButtonsVisible,
                        isSelectionViewVisible = areSelectionViewsVisible,
                        isMediaSelected = selectedFilesByMediaUid.containsKey(galleryMedia.uid),
                        itemScale = itemScale,
                    )
                )
            }
        }

        itemList.onNext(newItemList)
    }

    override fun onGalleryMediaItemClicked(item: GalleryListItem.Media) {
        log.debug {
            "onGalleryMediaItemClicked(): gallery_media_item_clicked:" +
                    "\nitem=$item"
        }

        val media = item.source
            ?: return

        when (currentState) {
            is State.Selecting -> {
                toggleMediaSelection(media)
            }

            is State.Viewing -> {
                openViewer(
                    media = media,
                    areActionsEnabled = true,
                )
            }
        }
    }

    override fun onGalleryMediaItemLongClicked(item: GalleryListItem.Media) {
        log.debug {
            "onGalleryMediaItemLongClicked(): gallery_media_item_long_clicked:" +
                    "\nitem=$item"
        }

        val media = item.source
            ?: return

        when (currentState) {
            State.Viewing -> {
                log.debug { "onGalleryMediaItemLongClicked(): switching_to_selecting" }

                switchFromViewingToSelecting(media)
            }

            else -> {
                // Long click does nothing in other states.
                log.debug { "onGalleryMediaItemLongClicked(): ignored" }
            }
        }
    }

    private fun toggleMediaSelection(media: GalleryMedia) {
        val currentState = this.currentState
        check(currentState is State.Selecting) {
            "Media selection can only be toggled in the corresponding state"
        }

        if (!currentState.allowMultiple) {
            if (media.files.size > 1) {
                openFileSelectionDialog(media.files)
            } else {
                onSingleMediaFileSelected?.invoke(media.files.firstOrNull().checkNotNull {
                    "There must be at least one file in the gallery media object"
                })
            }
        } else {
            if (selectedFilesByMediaUid.containsKey(media.uid)) {
                // When clicking currently selected media in the multiple selection state,
                // just unselect it.
                removeMediaFromSelection(media.uid)
            } else {
                if (media.files.size > 1) {
                    openFileSelectionDialog(media.files)
                } else {
                    addFileToSelection(media.files.firstOrNull().checkNotNull {
                        "There must be at least one file in the gallery media object"
                    })
                }
            }
        }
    }

    private fun addFileToSelection(file: GalleryMedia.File) {
        val currentState = this.currentState
        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Media file can only be added to the multiple selection in the corresponding state"
        }

        selectedFilesByMediaUid[file.mediaUid] = file

        log.debug {
            "addFileToSelection(): file_added:" +
                    "\nfile=$file," +
                    "\nmediaUid=${file.mediaUid}"
        }

        postGalleryItemsAsync(currentMediaRepository)
        postSelectedItemsCount()
    }

    private fun removeMediaFromSelection(mediaUid: String) {
        val currentState = this.currentState
        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Media can only be removed from the multiple selection in the corresponding state"
        }

        selectedFilesByMediaUid.remove(mediaUid)

        log.debug {
            "removeMediaFromSelection(): media_removed:" +
                    "\nmediaUid=$mediaUid"
        }

        if (selectedFilesByMediaUid.isEmpty() && canSwitchFromSelectingToViewing) {
            log.debug { "removeMediaFromSelection(): unselected_last_switching_to_viewing" }

            switchFromSelectingToViewing()
        } else {
            postGalleryItemsAsync(currentMediaRepository)
            postSelectedItemsCount()
        }
    }

    private fun postSelectedItemsCount() {
        selectedItemsCount.onNext(selectedFilesByMediaUid.keys.size)
    }

    override fun switchFromSelectingToViewing() {
        check(canSwitchFromSelectingToViewing) {
            "Switching is not allowed"
        }

        itemListState.onNext(State.Viewing)
        clearSelection()

        onSwitchedFromSelectingToViewing?.invoke()
    }

    private fun switchFromViewingToSelecting(startWith: GalleryMedia) {
        check(currentState is State.Viewing) {
            "Switching to selecting is only possible while viewing"
        }

        itemListState.onNext(
            State.Selecting(
                allowMultiple = true,
            )
        )

        toggleMediaSelection(startWith)

        // Post items with the new state.
        postGalleryItemsAsync(currentMediaRepository)

        onSwitchedFromViewingToSelecting?.invoke()
    }

    override fun onClearSelectionClicked() {
        val currentState = this.currentState

        check(currentState is State.Selecting && currentState.allowMultiple) {
            "Clear multiple selection button is only clickable in the corresponding state"
        }

        if (canSwitchFromSelectingToViewing) {
            switchFromSelectingToViewing()
        } else {
            clearSelection()
        }
    }

    override fun clearSelection() {
        selectedFilesByMediaUid.clear()
        postGalleryItemsAsync(currentMediaRepository)
        postSelectedItemsCount()
    }

    override fun onGalleryMediaItemViewClicked(item: GalleryListItem.Media) {
        log.debug {
            "onGalleryMediaItemViewClicked(): gallery_media_item_view_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            openViewer(
                media = item.source,
                areActionsEnabled = false,
            )
        }
    }

    private fun openViewer(
        media: GalleryMedia,
        areActionsEnabled: Boolean,
    ) {
        val index = currentMediaRepository.itemsList.indexOf(media)
        val repositoryParams = currentMediaRepository.params

        log.debug {
            "openViewer(): opening_viewer:" +
                    "\nmedia=$media," +
                    "\nindex=$index," +
                    "\nrepositoryParams=$repositoryParams," +
                    "\nareActionsEnabled=$areActionsEnabled"
        }

        itemListEvents.onNext(
            Event.OpenViewer(
                mediaIndex = index,
                repositoryParams = repositoryParams,
                areActionsEnabled = areActionsEnabled,
            )
        )
    }

    override fun onViewerReturnedLastViewedMediaIndex(lastViewedMediaIndex: Int) {
        // Find the media list item index considering there are other item types.
        var mediaListItemIndex = -1
        var listItemIndex = 0
        var mediaItemsCounter = 0
        for (item in itemList.value ?: emptyList()) {
            if (item is GalleryListItem.Media) {
                mediaItemsCounter++
            }
            if (mediaItemsCounter == lastViewedMediaIndex + 1) {
                mediaListItemIndex = listItemIndex
                break
            }
            listItemIndex++
        }

        // Ensure that the last viewed media is visible in the gallery list.
        if (mediaListItemIndex >= 0) {
            log.debug {
                "onViewerReturnedLastViewedMediaIndex(): ensuring_media_list_item_visibility:" +
                        "\nmediaIndex=$lastViewedMediaIndex" +
                        "\nmediaListItemIndex=$mediaListItemIndex"
            }

            itemListEvents.onNext(
                Event.EnsureListItemVisible(
                    listItemIndex = mediaListItemIndex,
                )
            )
        } else {
            log.error {
                "onViewerReturnedLastViewedMediaIndex(): cant_find_media_list_item_index:" +
                        "\nmediaIndex=$lastViewedMediaIndex"
            }
        }
    }

    private fun openFileSelectionDialog(files: List<GalleryMedia.File>) {
        log.debug {
            "openFileSelectionDialog(): posting_open_event:" +
                    "\nfiles=$files"
        }

        itemListEvents.onNext(Event.OpenFileSelectionDialog(files))
    }

    override fun onGalleryMediaFileSelected(file: GalleryMedia.File) {
        val currentState = this.currentState
        check(currentState is State.Selecting) {
            "Media files can only be selected in the selection state"
        }

        log.debug {
            "onFileSelected(): file_selected:" +
                    "\nfile=$file"
        }

        if (currentState.allowMultiple) {
            addFileToSelection(file)
        } else {
            onSingleMediaFileSelected?.invoke(file)
        }
    }
}
