package ua.com.radiokot.photoprism.features.map.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
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
    val source = MutableLiveData<GeoJsonSource>()

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
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { geoJson ->
                if (source.value != null) {
                    source.value!!.setGeoJson(geoJson)
                } else {
                    source.value =
                        GeoJsonSource(
                            id = SOURCE_ID,
                            geoJson = geoJson,
                            options =
                                GeoJsonOptions()
                                    .withCluster(true),
                        )
                }
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

    companion object {
        const val SOURCE_ID = "pp-photos"
    }
}
