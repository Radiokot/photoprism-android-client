package ua.com.radiokot.photoprism.features.map.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.maplibre.android.MapLibre
import org.maplibre.android.storage.FileSource
import org.maplibre.geojson.FeatureCollection
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.map.data.storage.MapGeoJsonRepository
import java.io.File

class MapViewModel(
    private val geoJsonRepository: MapGeoJsonRepository,
    private val mapCacheDirectory: File,
    application: Application,
) : AndroidViewModel(application) {

    private val log = kLogger("MapVM")
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    val isLoading = MutableLiveData(false)
    val featureCollection = MutableLiveData<FeatureCollection>()
    private var hasEverMovedCameraToSource = false
    val shouldMoveCameraToSource: Boolean
        get() = !hasEverMovedCameraToSource

    init {
        subscribeToRepository()
        update()
    }

    fun onPreparingForMapCreation() {
        MapLibre.getInstance(getApplication())

        if (!mapCacheDirectory.exists()) {
            mapCacheDirectory.mkdirs()
        }

        FileSource.setResourcesCachePath(
            mapCacheDirectory.path,
            object : FileSource.ResourcesCachePathChangeCallback {
                override fun onSuccess(path: String) {
                    log.debug {
                        "onPreparingForMapCreation(): cache_path_set:" +
                                "\npath=$path"
                    }
                }

                override fun onError(message: String) {
                    log.error {
                        "onPreparingForMapCreation(): failed_setting_cache_path:" +
                                "\nmessage=$message"
                    }
                }
            }
        )
    }

    private fun subscribeToRepository() {
        geoJsonRepository.loading
            .subscribe(isLoading::postValue)
            .autoDispose(this)

        geoJsonRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepository(): geojson_loading_failed"
                }

                eventsSubject.onNext(Event.ShowFloatingLoadingFailedError)
            }
            .autoDispose(this)

        geoJsonRepository.item
            .filter { !geoJsonRepository.isNeverUpdated }
            .observeOn(Schedulers.computation())
            .subscribe { geoJson ->
                val originalFeatureCollection = FeatureCollection.fromJson(geoJson)
                featureCollection.postValue(
                    // Reverse the features to see newest photos first
                    // in cluster thumbnails.
                    FeatureCollection.fromFeatures(
                        originalFeatureCollection
                            .features()!!
                            .reversed(),
                        originalFeatureCollection.bbox(),
                    )
                )
            }
            .autoDispose(this)
    }


    private fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            geoJsonRepository.update()
        } else {
            geoJsonRepository.updateIfNotFresh()
        }
    }

    fun onMovedCameraToSource() {
        hasEverMovedCameraToSource = true
    }

    fun onPhotoClicked(uid: String) {
        log.debug {
            "onPhotoClicked(): opening_viewer:" +
                    "\nphotoUid=$uid"
        }

        eventsSubject.onNext(
            Event.OpenViewer(
                repositoryParams = SimpleGalleryMediaRepository.Params(
                    query = "uid:$uid"
                ),
            )
        )
    }

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         */
        object ShowFloatingLoadingFailedError : Event

        class OpenViewer(
            val repositoryParams: SimpleGalleryMediaRepository.Params,
        ) : Event
    }
}
