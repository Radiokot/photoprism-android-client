package ua.com.radiokot.photoprism.features.map.view

import android.graphics.RectF
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMapBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaPreviewUrlFactory
import ua.com.radiokot.photoprism.util.images.ImageTransformations

class MapActivity : BaseActivity() {

    private val log = kLogger("MapActivity")
    private val viewModel: MapViewModel by viewModel()
    private lateinit var view: ActivityMapBinding
    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }
    private val picasso by inject<Picasso>()
    private val previewUrlFactory by inject<PhotoPrismMediaPreviewUrlFactory>()
    private val thumbnailSizePx = 150

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        // Must be initialized before inflating the view.
        MapLibre.getInstance(this)

        view = ActivityMapBinding.inflate(layoutInflater)
        setContentView(view.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        initFullScreen()
        initMap()
    }

    private fun initFullScreen() {
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun initMap() {
        val thumbnailSizePx = 150

        view.map.getMapAsync { map ->
            map.setStyle("https://cdn.photoprism.app/maps/default.json")
            map.cameraPosition = CameraPosition.DEFAULT

            map.getStyle { style ->
                viewModel
                    .source
                    .observe(this@MapActivity) { source ->
                        style.addSource(source)
                        // TODO: Set camera position corresponding to the source bounding box.
                    }

                style.addImage(
                    "placeholder",
                    ContextCompat.getDrawable(this, R.drawable.image_placeholder_circle)!!
                        .toBitmap(
                            width = thumbnailSizePx,
                            height = thumbnailSizePx,
                        )
                )

                val clusterLayer =
                    CircleLayer("pp-cluster-circles", MapViewModel.SOURCE_ID)
                        .withProperties(
                            PropertyFactory.circleColor("#FF9800".toColorInt()),
                            PropertyFactory.circleRadius(10.0f),
                        )
                        .withFilter(Expression.has("point_count"))
                        .also(style::addLayer)

                val photoLayer =
                    SymbolLayer("pp-photos", MapViewModel.SOURCE_ID)
                        .withProperties(
                            PropertyFactory.iconImage(
                                Expression.coalesce(
                                    Expression.image(Expression.get("Hash")),
                                    Expression.image(Expression.literal("placeholder"))
                                )
                            ),
                            PropertyFactory.iconSize(1f),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true)
                        )
                        .withFilter(Expression.not(Expression.has("point_count")))
                        .also(style::addLayer)

                map.addOnCameraIdleListener {
                    updatePhotoThumbnails(map, style, photoLayer)
                }

                log.debug {
                    "initMap(): style_initialized"
                }
            }

            log.debug {
                "initMap(): map_initialized"
            }
        }
    }

    private var photoThumbnailsUpdateDisposable: Disposable? = null
    private fun updatePhotoThumbnails(
        map: MapLibreMap,
        style: Style,
        photoLayer: SymbolLayer,
    ) {
        photoThumbnailsUpdateDisposable?.dispose()

        val visiblePhotoFeatures = map.queryRenderedFeatures(
            map.projection.visibleRegion.latLngBounds.toRectF(map),
            photoLayer.id,
        )

        log.debug {
            "updatePhotoThumbnails(): updating:" +
                    "\nvisiblePhotoFeatures=${visiblePhotoFeatures.size}"
        }

        val visibleThumbnailHashes =
            visiblePhotoFeatures
                .mapTo(mutableSetOf()) { feature ->
                    feature.getProperty("Hash").asString
                }

        photoThumbnailsUpdateDisposable =
            visibleThumbnailHashes
                .filter { style.getImage(it) == null }
                .map { thumbnailHash ->
                    picasso
                        .load(
                            previewUrlFactory.getThumbnailUrl(
                                thumbnailHash = thumbnailHash,
                                sizePx = thumbnailSizePx,
                            )
                        )
                        .resize(thumbnailSizePx, thumbnailSizePx)
                        .transform(ImageTransformations.circle)
                        .intoSingle()
                        .doOnSuccess { bitmap ->
                            style.addImage(thumbnailHash, bitmap)
                            photoLayer.setProperties(
                                PropertyFactory.iconSize(
                                    photoLayer.iconSize.value.let { currentValue ->
                                        if (currentValue > 1f)
                                            currentValue - 0.0001f
                                        else
                                            currentValue + 0.0001f
                                    }
                                )
                            )
                        }
                        .ignoreElement()
                }
                .let(Completable::concatDelayError)
                .doOnEvent {
                    log.debug {
                        "updatePhotoThumbnails(): updated"
                    }
                }
                .subscribeBy()
                .autoDispose(this)
    }

    override fun onStart() {
        super.onStart()
        view.map.onStart()
    }

    override fun onResume() {
        super.onResume()
        view.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        view.map.onPause()
    }

    override fun onStop() {
        super.onStop()
        view.map.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        view.map.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        view.map.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        view.map.onSaveInstanceState(outState)
    }

    private fun LatLngBounds.toRectF(map: MapLibreMap): RectF {
        val projection = map.projection
        val ne = projection.toScreenLocation(northEast)
        val sw = projection.toScreenLocation(southWest)
        return RectF(sw.x, ne.y, ne.x, sw.y)
    }
}
