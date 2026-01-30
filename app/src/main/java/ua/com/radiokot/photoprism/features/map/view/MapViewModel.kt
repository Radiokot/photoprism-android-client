package ua.com.radiokot.photoprism.features.map.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.maplibre.geojson.FeatureCollection
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.map.data.storage.MapGeoJsonRepository

class MapViewModel(
    private val geoJsonRepository: MapGeoJsonRepository,
) : ViewModel() {

    private val log = kLogger("MapVM")
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    val isLoading = MutableLiveData(false)
    val featureCollection = MutableLiveData<FeatureCollection>()

    init {
        subscribeToRepository()

        update()
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

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         */
        object ShowFloatingLoadingFailedError : Event
    }
}
