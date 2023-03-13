package ua.com.radiokot.photoprism.features.viewer.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import ua.com.radiokot.photoprism.extension.addToCloseables
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPageItem

class MediaViewerViewModel(
    private val galleryMediaRepositoryFactory: SimpleGalleryMediaRepository.Factory,
) : ViewModel() {
    private val log = kLogger("MediaViewerVM")
    private lateinit var galleryMediaRepository: SimpleGalleryMediaRepository

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val itemsList: MutableLiveData<List<MediaViewerPageItem>?> = MutableLiveData(null)

    fun init(repositoryKey: String) {
        galleryMediaRepository = galleryMediaRepositoryFactory.get(repositoryKey)
            .checkNotNull {
                "The repository must be created beforehand"
            }

        subscribeToRepository()

        log.debug {
            "init(): initialized:" +
                    "\nrepositoryKey=$repositoryKey"
        }

        update()
    }

    private fun subscribeToRepository() {
        galleryMediaRepository.items
            .observeOn(AndroidSchedulers.mainThread())
            .map { galleryMediaItems ->
                galleryMediaItems.map(MediaViewerPageItem.Companion::fromGalleryMedia)
            }
            .subscribe(itemsList::setValue)
            .addToCloseables(this)

        galleryMediaRepository.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(isLoading::setValue)
            .addToCloseables(this)

        galleryMediaRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                log.error(it) { "subscribeToRepository(): error_occurred" }
            }
            .addToCloseables(this)
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
}