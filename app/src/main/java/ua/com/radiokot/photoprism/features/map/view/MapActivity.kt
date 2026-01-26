package ua.com.radiokot.photoprism.features.map.view

import android.os.Bundle
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMapBinding
import ua.com.radiokot.photoprism.extension.kLogger

class MapActivity : BaseActivity() {

    private val log = kLogger("MapActivity")
    private val viewModel: MapViewModel by viewModel()
    private lateinit var view: ActivityMapBinding
    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }

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

                style.addLayer(
                    CircleLayer("pp-photo-circles", MapViewModel.SOURCE_ID).apply {
                        setProperties(
                            PropertyFactory.circleColor("#FF9800".toColorInt()),
                            PropertyFactory.circleRadius(10.0f),
                        )
                        setFilter(Expression.eq(Expression.get("cluster"), true))
                    }
                )

                style.addLayer(
                    CircleLayer("unclustered-points", MapViewModel.SOURCE_ID).apply {
                        setProperties(
                            PropertyFactory.circleColor("#FF0000".toColorInt()),
                            PropertyFactory.circleRadius(10.0f),
                        )
                        setFilter(Expression.neq(Expression.get("cluster"), true))
                    }
                )

                log.debug {
                    "initMap(): style_initialized"
                }
            }

            log.debug {
                "initMap(): map_initialized"
            }
        }
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
}
