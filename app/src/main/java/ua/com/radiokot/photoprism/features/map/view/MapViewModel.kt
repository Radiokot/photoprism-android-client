package ua.com.radiokot.photoprism.features.map.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.maplibre.android.constants.MapLibreConstants
import org.maplibre.android.style.expressions.Expression.accumulated
import org.maplibre.android.style.expressions.Expression.concat
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.length
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.lt
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
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

    private fun createClusteredSource(geoJson: String): GeoJsonSource {
        val featureCollection =
            FeatureCollection.fromJson(geoJson)
        val newestFirstFeatureCollection =
            FeatureCollection.fromFeatures(featureCollection.features()!!.reversed())

        return GeoJsonSource(
            id = SOURCE_ID,
            features = newestFirstFeatureCollection,
            options =
                GeoJsonOptions()
                    .withCluster(true)
                    // Setting this to max zoom prevents declustering hundreds of photos
                    // taken at the same location.
                    .withClusterMaxZoom(MapLibreConstants.MAXIMUM_ZOOM.toInt())
                    .withClusterRadius(80)
                    // This expression collects enough photo hashes
                    // from the cluster to create a thumbnail.
                    // The hashes are comma separated, with a trailing comma.
                    // The magic number ✨ is obtained through trial and error.
                    .withClusterProperty(
                        "Hashes",
                        switchCase(
                            lt(length(accumulated()), 150),
                            concat(accumulated(), get("Hashes")),
                            accumulated()
                        ),
                        concat(
                            get("Hash"),
                            literal(",")
                        ),
                    ),
        )
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
