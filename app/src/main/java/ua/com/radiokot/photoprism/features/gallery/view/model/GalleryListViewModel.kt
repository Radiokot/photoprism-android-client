package ua.com.radiokot.photoprism.features.gallery.view.model

import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository

interface GalleryListViewModel {

    val itemListState: BehaviorSubject<State>
    val itemListEvents: PublishSubject<Event>
    val itemList: BehaviorSubject<List<GalleryListItem>>
    val itemScale: BehaviorSubject<GalleryItemScale>
    val selectedItemsCount: BehaviorSubject<Int>
    val selectedFilesByMediaUid: LinkedHashMap<String, GalleryMedia.File>

    fun postGalleryItemsAsync(repository: SimpleGalleryMediaRepository)
    fun postGalleryItems(repository: SimpleGalleryMediaRepository)
    fun clearSelection()
    fun switchFromSelectingToViewing()

    fun onGalleryMediaItemClicked(item: GalleryListItem.Media)
    fun onGalleryMediaItemViewClicked(item: GalleryListItem.Media)
    fun onGalleryMediaItemLongClicked(
        item: GalleryListItem.Media,
        globalPosition: Int,
    )

    fun onGalleryMediaItemsDragSelectionChanged(itemSelection: Sequence<Pair<GalleryListItem.Media, Boolean>>)
    fun onViewerReturnedLastViewedMediaIndex(lastViewedMediaIndex: Int)
    fun onClearSelectionClicked()

    sealed interface Event {
        class OpenViewer(
            val mediaIndex: Int,
            val repositoryParams: SimpleGalleryMediaRepository.Params,
            val areActionsEnabled: Boolean,
        ) : Event

        /**
         * Ensure that the given item of the [itemList] is visible on the screen.
         */
        class EnsureListItemVisible(val listItemIndex: Int) : Event

        class ActivateDragSelection(val startGlobalPosition: Int) : Event
    }

    sealed interface State {
        /**
         * Viewing the gallery content.
         */
        object Viewing : State

        /**
         * Viewing the gallery content to select something.
         */
        class Selecting(
            /**
             * Whether selection of multiple items is allowed or not.
             */
            val allowMultiple: Boolean,
        ) : State
    }
}
