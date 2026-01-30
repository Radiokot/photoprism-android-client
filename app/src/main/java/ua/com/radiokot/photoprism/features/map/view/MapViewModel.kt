package ua.com.radiokot.photoprism.features.map.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.maplibre.android.style.expressions.Expression.accumulated
import org.maplibre.android.style.expressions.Expression.concat
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.length
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.lt
import org.maplibre.android.style.expressions.Expression.switchCase
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
                    source.value = createClusteredSource(geoJson)
                }
            }
            .autoDispose(this)
    }

    private fun createClusteredSource(geoJson: String): GeoJsonSource =
        GeoJsonSource(
            id = SOURCE_ID,
            geoJson = geoJson,
            options =
                GeoJsonOptions()
                    .withCluster(true)
                    // This expression collects up to 4 photo hashes from the cluster, separated by a comma.
                    // 80 is a magic ✨ number obtained through trial and error.
                    .withClusterProperty(
                        "Hashes",
                        switchCase(
                            lt(length(accumulated()), 80),
                            concat(accumulated(), get("Hashes")),
                            accumulated()
                        ),
                        concat(
                            get("Hash"),
                            literal(",")
                        ),
                    ),
        )

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
