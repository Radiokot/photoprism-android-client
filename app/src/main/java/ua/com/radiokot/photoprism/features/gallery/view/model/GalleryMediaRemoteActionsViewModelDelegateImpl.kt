package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.albums.data.model.DestinationAlbum
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.AddGalleryMediaToAlbumUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.ArchiveGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.DeleteGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.RemoveGalleryMediaFromAlbumUseCase
import ua.com.radiokot.photoprism.features.viewer.logic.UpdateGalleryMediaAttributesUseCase

class GalleryMediaRemoteActionsViewModelDelegateImpl(
    private val archiveGalleryMediaUseCase: ArchiveGalleryMediaUseCase,
    private val deleteGalleryMediaUseCase: DeleteGalleryMediaUseCase,
    private val updateGalleryMediaAttributesUseCase: UpdateGalleryMediaAttributesUseCase,
    private val addGalleryMediaToAlbumUseCase: AddGalleryMediaToAlbumUseCase,
    private val removeGalleryMediaFromAlbumUseCase: RemoveGalleryMediaFromAlbumUseCase,
) : ViewModel(),
    GalleryMediaRemoteActionsViewModelDelegate {

    private val log = kLogger("GalleryMediaRemoteActionsVMDI")

    override val galleryMediaRemoteActionsEvents: PublishSubject<GalleryMediaRemoteActionsViewModel.Event> =
        PublishSubject.create()

    private var doOnDeletingConfirmed = { }
    private var doOnAlbumForAddingSelected = { _: DestinationAlbum -> }

    override fun deleteGalleryMedia(
        mediaUids: Collection<String>,
        currentMediaRepository: SimpleGalleryMediaRepository,
        onStarted: () -> Unit,
    ) {
        fun doDelete() {
            deleteGalleryMediaUseCase
                .invoke(
                    mediaUids = mediaUids,
                    currentGalleryMediaRepository = currentMediaRepository,
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    log.debug {
                        "deleteGalleryMedia::doDelete(): start_deleting:" +
                                "\nitems=${mediaUids.size}"
                    }

                    onStarted()
                }
                .subscribeBy(
                    onError = { error ->
                        log.error(error) {
                            "deleteGalleryMedia::doDelete(): failed_deleting"
                        }
                    },
                    onComplete = {
                        log.debug {
                            "deleteGalleryMedia::doDelete(): successfully_deleted"
                        }
                    }
                )
                .autoDispose(this)
        }

        doOnDeletingConfirmed = ::doDelete

        galleryMediaRemoteActionsEvents.onNext(
            GalleryMediaRemoteActionsViewModel.Event.OpenDeletingConfirmationDialog
        )
    }

    override fun archiveGalleryMedia(
        mediaUids: Collection<String>,
        currentMediaRepository: SimpleGalleryMediaRepository,
        onStarted: () -> Unit,
    ) {
        archiveGalleryMediaUseCase
            .invoke(
                mediaUids = mediaUids,
                currentGalleryMediaRepository = currentMediaRepository,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                log.debug {
                    "archiveGalleryMedia(): start_archiving:" +
                            "\nitems=${mediaUids.size}"
                }

                onStarted()
            }
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "archiveGalleryMedia(): failed_archiving"
                    }
                },
                onComplete = {
                    log.debug {
                        "archiveGalleryMedia(): successfully_archived"
                    }
                }
            )
            .autoDispose(this)
    }

    override fun addGalleryMediaToAlbum(
        mediaUids: Collection<String>,
        onStarted: () -> Unit,
    ) {
        fun doAddToAlbum(album: DestinationAlbum) {
            addGalleryMediaToAlbumUseCase
                .invoke(
                    mediaUids = mediaUids,
                    destinationAlbum = album,
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    log.debug {
                        "addGalleryMediaToAlbum::doAddToAlbum(): start_adding:" +
                                "\nitems=${mediaUids.size}," +
                                "\nalbum=$album"
                    }

                    onStarted()
                }
                .subscribeBy(
                    onError = { error ->
                        log.error(error) {
                            "addGalleryMediaToAlbum::doAddToAlbum(): failed_adding"
                        }
                    },
                    onComplete = {
                        log.debug {
                            "addGalleryMediaToAlbum::doAddToAlbum(): successfully_added"
                        }

                        galleryMediaRemoteActionsEvents.onNext(
                            GalleryMediaRemoteActionsViewModel.Event.ShowFloatingAddedToAlbumMessage(
                                albumTitle = album.title,
                            )
                        )
                    }
                )
                .autoDispose(this)
        }

        doOnAlbumForAddingSelected = ::doAddToAlbum

        galleryMediaRemoteActionsEvents.onNext(
            GalleryMediaRemoteActionsViewModel.Event.OpenAlbumForAddingSelection
        )
    }

    override fun removeGalleryMediaFromAlbum(
        mediaUids: Collection<String>,
        albumUid: String,
        onStarted: () -> Unit,
    ) {
        removeGalleryMediaFromAlbumUseCase
            .invoke(
                mediaUids = mediaUids,
                albumUid = albumUid,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                log.debug {
                    "removeGalleryMediaFromAlbum(): start_removing:" +
                            "\nitems=${mediaUids.size}," +
                            "\nalbumUid=$albumUid"
                }

                onStarted()
            }
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "removeGalleryMediaFromAlbum(): failed_removing"
                    }
                },
                onComplete = {
                    log.debug {
                        "removeGalleryMediaFromAlbum(): physically_removed"
                    }
                }
            )
            .autoDispose(this)
    }

    override fun updateGalleryMediaAttributes(
        mediaUid: String,
        currentMediaRepository: SimpleGalleryMediaRepository,
        isFavorite: Boolean?,
        isPrivate: Boolean?,
        onStarted: () -> Unit,
        onUpdated: () -> Unit,
    ) {
        updateGalleryMediaAttributesUseCase
            .invoke(
                mediaUid = mediaUid,
                isFavorite = isFavorite,
                isPrivate = isPrivate,
                currentGalleryMediaRepository = currentMediaRepository,
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                log.debug {
                    "updateGalleryMediaAttributes(): start_updating:" +
                            "\nitem=$mediaUid," +
                            "\nisFavorite=$isFavorite," +
                            "\nisPrivate=$isPrivate"
                }

                onStarted()
            }
            .subscribeBy(
                onError = { error ->
                    log.error(error) {
                        "updateGalleryMediaAttributes(): failed_updating"
                    }
                },
                onComplete = {
                    log.debug {
                        "updateGalleryMediaAttributes(): successfully_updated"
                    }

                    onUpdated()
                }
            )
            .autoDispose(this)
    }

    override fun onDeletingGalleryMediaConfirmed() =
        doOnDeletingConfirmed()

    override fun onAlbumForAddingGalleryMediaSelected(selectedAlbum: DestinationAlbum) =
        doOnAlbumForAddingSelected(selectedAlbum)
}
