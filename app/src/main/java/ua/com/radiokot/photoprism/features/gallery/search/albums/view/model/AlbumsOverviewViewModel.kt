package ua.com.radiokot.photoprism.features.gallery.search.albums.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.storage.AlbumsRepository
import java.util.concurrent.TimeUnit

class AlbumsOverviewViewModel(
    private val albumsRepository: AlbumsRepository,
    private val filterPredicate: (album: Album, filter: String) -> Boolean,
) : ViewModel() {
    private val log = kLogger("AlbumsOverviewVM")

    val selectedAlbumUid = MutableLiveData<String?>()
    val isLoading = MutableLiveData(false)
    val albums = MutableLiveData<List<AlbumListItem>>()
    val isLoadingFailed = MutableLiveData(false)

    /**
     * Raw input of the search view.
     */
    val filterInput = MutableLiveData("")

    init {
        subscribeToRepository()
        subscribeToFilter()
        subscribeToAlbumSelection()

        update()
    }

    fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            albumsRepository.update()
        } else {
            albumsRepository.updateIfNotFresh()
        }
    }

    private fun subscribeToRepository() {
        albumsRepository.items
            .filter { !albumsRepository.isNeverUpdated }
            .subscribe { postAlbumItems() }
            .autoDispose(this)

        albumsRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepository(): albums_loading_failed"
                }

                if (albums.value == null) {
                    isLoadingFailed.value = true
                } else {
                    isLoadingFailed.value = false
                    // TODO show floating error
                }
            }
            .autoDispose(this)

        albumsRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(isLoading::setValue)
            .autoDispose(this)
    }

    private fun subscribeToFilter() {
        Observable
            .create { emitter ->
                filterInput.observeForever(emitter::onNext)
            }
            .debounce { value ->
                // Apply debounce to the input unless it is empty (input is cleared).
                if (value.isEmpty())
                    Observable.just(0L)
                else
                    Observable.timer(400, TimeUnit.MILLISECONDS)
            }
            // Only react to the albums are loaded.
            .filter { albums.value != null }
            .subscribe { postAlbumItems() }
            .autoDispose(this)
    }

    private fun subscribeToAlbumSelection() {
        selectedAlbumUid.observeForever {
            if (albums.value != null) {
                postAlbumItems()
            }
        }
    }

    /**
     * Posts filtered album items in the main thread.
     */
    private fun postAlbumItems() {
        val repositoryAlbums = albumsRepository.itemsList
        val filter = filterInput.value?.takeIf(String::isNotEmpty)
        val filteredRepositoryAlbums =
            if (filter != null)
                repositoryAlbums.filter { album ->
                    filterPredicate(album, filter)
                }
            else
                repositoryAlbums
        val selectedAlbumUid = selectedAlbumUid.value

        log.debug {
            "postAlbumItems(): posting_items:" +
                    "\nalbumsCount=${repositoryAlbums.size}," +
                    "\nselectedAlbumUid=$selectedAlbumUid," +
                    "\nfilter=$filter," +
                    "\nfilteredAlbumsCount=${filteredRepositoryAlbums.size}"
        }

        albums.postValue(
            filteredRepositoryAlbums.map { album ->
                AlbumListItem(
                    source = album,
                    isAlbumSelected = album.uid == selectedAlbumUid,
                )
            }
        )
    }

    fun onAlbumItemClicked(item: AlbumListItem) {
        log.debug {
            "onAlbumItemClicked(): album_item_clicked:" +
                    "\nitem=$item"
        }

        if (item.source != null) {
            val uid = item.source.uid

            val newSelectedAlbumUid: String? =
                if (selectedAlbumUid.value != uid)
                    uid
                else
                    null

            log.debug {
                "onAlbumItemClicked(): setting_selected_album_uid:" +
                        "\nnewUid=$newSelectedAlbumUid"
            }

            selectedAlbumUid.value = newSelectedAlbumUid
        }
    }

    fun onRetryClicked() {
        log.debug {
            "onRetryClicked(): updating"
        }

        update()
    }

    fun onSwipeRefreshPulled() {
        log.debug {
            "onRetryClicked(): force_updating"
        }

        update(force = true)
    }
}
