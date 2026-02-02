package ua.com.radiokot.photoprism.features.map.view

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.toObservable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.constants.MapLibreConstants
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.FormatOption.formatFontScale
import org.maplibre.android.style.expressions.Expression.NumberFormatOption.locale
import org.maplibre.android.style.expressions.Expression.NumberFormatOption.maxFractionDigits
import org.maplibre.android.style.expressions.Expression.accumulated
import org.maplibre.android.style.expressions.Expression.coalesce
import org.maplibre.android.style.expressions.Expression.concat
import org.maplibre.android.style.expressions.Expression.division
import org.maplibre.android.style.expressions.Expression.format
import org.maplibre.android.style.expressions.Expression.formatEntry
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.image
import org.maplibre.android.style.expressions.Expression.length
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.lt
import org.maplibre.android.style.expressions.Expression.neq
import org.maplibre.android.style.expressions.Expression.not
import org.maplibre.android.style.expressions.Expression.numberFormat
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.BoundingBox
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMapBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.view.GallerySingleRepositoryActivity
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity
import ua.com.radiokot.photoprism.util.FullscreenInsetsCompat
import ua.com.radiokot.photoprism.util.images.ImageTransformations
import java.util.Locale
import java.util.concurrent.TimeUnit

class MapActivity : BaseActivity() {

    private val log = kLogger("MapActivity")
    private val viewModel: MapViewModel by viewModel()
    private lateinit var view: ActivityMapBinding
    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }
    private val picasso by inject<Picasso>()
    private val previewUrlFactory by inject<PhotoPrismMediaPreviewUrlFactory>()
    private val thumbnailSizePx: Int by lazy {
        resources.getDimensionPixelSize(R.dimen.map_image_width)
    }
    private val thumbnailTransformation: Transformation by lazy {
        ImageTransformations.roundedCorners(
            cornerRadiusDp = 8,
            context = this,
        )
    }
    private val locale: Locale by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        // Must be initialized before inflating the view.
        viewModel.onPreparingForMapCreation()

        view = ActivityMapBinding.inflate(layoutInflater)
        setContentView(view.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        initToolbar()
        initFullScreen()

        view.map.onCreate(savedInstanceState)
        initMap()

        subscribeToEvents()
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        view.toolbar.doOnPreDraw {
            val insets = FullscreenInsetsCompat.getForTranslucentSystemBars(view.toolbar)
            view.toolbar.updateLayoutParams {
                this as ViewGroup.MarginLayoutParams
                setMargins(
                    insets.left,
                    insets.top,
                    insets.right,
                    this.bottomMargin,
                )
            }
        }
    }

    private fun initFullScreen() {
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun initMap() = view.map.getMapAsync { map ->
        map.setMaxZoomPreference(20.0)
        map.setStyle(getKoin().getProperty<String>("defaultMapStyleUrl"))

        map.getStyle { style ->
            initMapStyle(map, style)
        }

        initMapInsets(map)

        log.debug {
            "initMap(): initialized"
        }
    }

    private fun initMapStyle(
        map: MapLibreMap,
        style: Style,
    ) {
        viewModel
            .featureCollection
            .observe(this@MapActivity) { featureCollection ->
                style.addSource(createClusteredSource(featureCollection))

                if (viewModel.shouldMoveCameraToSource) {
                    val boundingBox = checkNotNull(featureCollection.bbox()) {
                        "There must be the bounding box"
                    }
                    map.easeCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            bounds = boundingBox.toLatLngBounds(),
                            padding = thumbnailSizePx / 2,
                        )
                    )
                    viewModel.onMovedCameraToSource()
                }
            }

        // Localization of names.
        style.layers.forEach { layer ->
            if (layer is SymbolLayer && layer.textField.toString().contains("name:")) {
                layer.setProperties(
                    textField(
                        switchCase(
                            neq(
                                coalesce(
                                    get("name:${locale.language}"),
                                    get("name")
                                ),
                                get("name")
                            ),
                            format(
                                formatEntry(get("name:${locale.language}")),
                                formatEntry("\n"),
                                formatEntry(
                                    get("name"),
                                    formatFontScale(0.8)
                                )
                            ),
                            get("name")
                        )
                    )
                )
            }
        }

        style.addImage(
            "placeholder",
            ContextCompat.getDrawable(this, R.drawable.image_placeholder)!!
                .toBitmap(
                    width = thumbnailSizePx,
                    height = thumbnailSizePx,
                )
                .let(thumbnailTransformation::transform)
        )

        val clusterLayer =
            SymbolLayer("pp-clusters", SOURCE_ID)
                .withProperties(
                    // Cluster thumbnail – abbreviatedCount + ':' + a few comma-separated photo hashes
                    // from the 'Hashes' cluster property defined during the source creation.
                    iconImage(
                        coalesce(
                            image(get("Hashes")),
                            image(Expression.literal("placeholder"))
                        )
                    ),
                    iconSize(1f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    textField(
                        // 'point_count_abbreviated' has improper format,
                        // so a custom expression is used instead.
                        switchCase(
                            lt(get("point_count"), 1000),
                            get("point_count"),
                            concat(
                                numberFormat(
                                    division(
                                        get("point_count"),
                                        literal(1000)
                                    ),
                                    locale(locale.toString()),
                                    maxFractionDigits(1),
                                ),
                                literal("k"),
                            )
                        )
                    ),
                    textFont(arrayOf("Noto Sans Regular")),
                    textSize(15f),
                    textColor(
                        ContextCompat.getColor(
                            this,
                            R.color.md_theme_light_background
                        )
                    ),
                    textHaloColor(
                        ContextCompat.getColor(
                            this,
                            R.color.md_theme_light_outline
                        )
                    ),
                    textHaloWidth(1.25f),
                )
                .withFilter(has("point_count"))
                .also(style::addLayer)

        val photoLayer =
            SymbolLayer("pp-photos", SOURCE_ID)
                .withProperties(
                    iconImage(
                        coalesce(
                            image(get("Hash")),
                            image(Expression.literal("placeholder"))
                        )
                    ),
                    iconSize(1f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                )
                .withFilter(not(has("point_count")))
                .also(style::addLayer)

        initThumbnailsLoading(
            map = map,
            style = style,
            photoLayer = photoLayer,
            clusterLayer = clusterLayer,
        )
        initMapClicks(
            map = map,
            photoLayer = photoLayer,
            clusterLayer = clusterLayer,
        )

        log.debug {
            "initMapStyle(): initialized"
        }
    }

    private fun initMapInsets(
        map: MapLibreMap,
    ) = view.map.doOnPreDraw {
        val insets = FullscreenInsetsCompat.getForTranslucentSystemBars(view.map)
        with(map.uiSettings) {
            val attributionMargin =
                resources.getDimensionPixelSize(R.dimen.map_attribution_margin)
            logoGravity = Gravity.BOTTOM or Gravity.START
            setLogoMargins(
                attributionMargin + insets.left,
                attributionMargin,
                attributionMargin + insets.right,
                attributionMargin + insets.bottom,
            )
            attributionGravity = Gravity.BOTTOM or Gravity.END
            setAttributionMargins(
                attributionMargin + insets.left,
                attributionMargin,
                attributionMargin + insets.right,
                attributionMargin + insets.bottom,
            )

            val compassMargin =
                resources.getDimensionPixelSize(R.dimen.map_compass_margin)
            compassGravity = Gravity.TOP or Gravity.END
            setCompassMargins(
                compassMargin + insets.left,
                compassMargin + insets.top,
                compassMargin + insets.right,
                compassMargin,
            )
        }
    }

    private fun initThumbnailsLoading(
        map: MapLibreMap,
        style: Style,
        photoLayer: SymbolLayer,
        clusterLayer: SymbolLayer,
    ) {
        val thumbnailsBeingLoaded = mutableSetOf<String>()
        val contentUpdates = PublishSubject.create<Unit>()
        val photoLayerInvalidations = PublishSubject.create<Unit>()
        val clusterLayerInvalidations = PublishSubject.create<Unit>()

        view.map.addOnDidFinishRenderingFrameListener { fully, _, _ ->
            if (fully) {
                contentUpdates.onNext(Unit)
            }
        }

        contentUpdates
            .throttleLast(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                map.queryRenderedFeatures(
                    map.projection.visibleRegion.latLngBounds.toRectF(map),
                    photoLayer.id,
                    clusterLayer.id,
                )
            }
            .distinctUntilChanged()
            .flatMap { visibleFeatures ->
                log.debug {
                    "initThumbnailsLoading(): visible_features_changed"
                }

                visibleFeatures
                    .mapTo(mutableSetOf()) { feature ->
                        if (feature.hasProperty("Hashes"))
                            feature.getStringProperty("Hashes")
                        else
                            feature.getStringProperty("Hash")
                    }
                    .filter { thumbnailId ->
                        style.getImage(thumbnailId) == null
                                && thumbnailId !in thumbnailsBeingLoaded
                    }
                    .toObservable()
            }
            .subscribe { thumbnailId ->
                thumbnailsBeingLoaded += thumbnailId

                log.debug {
                    "initThumbnailsLoading(): start_loading:" +
                            "\nid=$thumbnailId"
                }

                val isCluster = thumbnailId.contains(',')

                val getThumbnailBitmap =
                    if (isCluster)
                        getClusterThumbnailBitmap(thumbnailId)
                    else
                        getPhotoThumbnailBitmap(thumbnailId)

                getThumbnailBitmap
                    .doOnEvent { _, error ->
                        thumbnailsBeingLoaded -= thumbnailId
                        log.debug {
                            "initThumbnailsLoading(): loading_finished:" +
                                    "\nid=$thumbnailId," +
                                    "\nsuccess=${error == null}"
                        }
                    }
                    .subscribeBy { thumbnailBitmap ->
                        style.addImage(thumbnailId, thumbnailBitmap)
                        if (isCluster) {
                            clusterLayerInvalidations.onNext(Unit)
                        } else {
                            photoLayerInvalidations.onNext(Unit)
                        }
                    }
                    .autoDispose(this)
            }
            .autoDispose(this)

        photoLayerInvalidations
            .throttleLast(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { photoLayer.invalidate() }
            .autoDispose(this)

        clusterLayerInvalidations
            .throttleLast(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { clusterLayer.invalidate() }
            .autoDispose(this)
    }

    private fun createClusteredSource(featureCollection: FeatureCollection): GeoJsonSource =
        GeoJsonSource(
            id = SOURCE_ID,
            features = featureCollection,
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
                    )
                    // Cluster bounds.
                    .withClusterProperty(
                        "LatNorth",
                        Expression.max(accumulated(), get("LatNorth")),
                        get("Lat")
                    )
                    .withClusterProperty(
                        "LngEast",
                        Expression.max(accumulated(), get("LngEast")),
                        get("Lng")
                    )
                    .withClusterProperty(
                        "LatSouth",
                        Expression.min(accumulated(), get("LatSouth")),
                        get("Lat")
                    )
                    .withClusterProperty(
                        "LngWest",
                        Expression.min(accumulated(), get("LngWest")),
                        get("Lng")
                    ),
        )

    private fun getPhotoThumbnailBitmap(
        thumbnailHash: String,
    ): Single<Bitmap> =
        picasso
            .load(
                previewUrlFactory.getThumbnailUrl(
                    thumbnailHash = thumbnailHash,
                    sizePx = thumbnailSizePx,
                )
            )
            .resize(thumbnailSizePx, thumbnailSizePx)
            .transform(thumbnailTransformation)
            .intoSingle()

    private fun getClusterThumbnailBitmap(
        thumbnailId: String
    ): Single<Bitmap> {
        var thumbnailHashes =
            thumbnailId
                .split(',')
                .filter(String::isNotEmpty)

        thumbnailHashes =
            if (thumbnailHashes.size >= 4)
                thumbnailHashes.take(4)
            else
                thumbnailHashes.take(2)

        val composeTiles =
            if (thumbnailHashes.size == 4) {
                val tileSize = thumbnailSizePx / 2
                thumbnailHashes
                    .map { thumbnailHash ->
                        picasso
                            .load(
                                previewUrlFactory.getThumbnailUrl(
                                    thumbnailHash = thumbnailHash,
                                    sizePx = tileSize,
                                )
                            )
                            .resize(tileSize, tileSize)
                            .intoSingle()
                    }
                    .let(Single<Bitmap>::concatDelayError)
                    .toList()
                    .map(::composeFourTiles)
            } else {
                thumbnailHashes
                    .map { thumbnailHash ->
                        picasso
                            .load(
                                previewUrlFactory.getThumbnailUrl(
                                    thumbnailHash = thumbnailHash,
                                    sizePx = thumbnailSizePx,
                                )
                            )
                            .resize(thumbnailSizePx, thumbnailSizePx)
                            .intoSingle()
                    }
                    .let(Single<Bitmap>::concatDelayError)
                    .toList()
                    .map(::composeTwoTiles)
            }

        return composeTiles
            .map(thumbnailTransformation::transform)
    }

    private fun composeFourTiles(fourTiles: List<Bitmap>): Bitmap {
        val size = fourTiles[0].width
        val resultBitmap = createBitmap(size * 2, size * 2)
        val canvas = Canvas(resultBitmap)

        canvas.drawBitmap(fourTiles[0], 0f, 0f, null)
        canvas.drawBitmap(fourTiles[1], size.toFloat(), 0f, null)
        canvas.drawBitmap(fourTiles[2], 0f, size.toFloat(), null)
        canvas.drawBitmap(fourTiles[3], size.toFloat(), size.toFloat(), null)

        return resultBitmap
    }

    private fun composeTwoTiles(twoTiles: List<Bitmap>): Bitmap {
        val tileSize = twoTiles[0].width
        val resultSize = tileSize
        val resultBitmap = createBitmap(resultSize, resultSize)
        val canvas = Canvas(resultBitmap)

        val cropAmount = tileSize / 4
        val srcRect = Rect(cropAmount, 0, tileSize - cropAmount, tileSize)
        val dstRectLeft = RectF(0f, 0f, resultSize / 2f, resultSize.toFloat())
        val dstRectRight = RectF(resultSize / 2f, 0f, resultSize.toFloat(), resultSize.toFloat())

        canvas.drawBitmap(twoTiles[0], srcRect, dstRectLeft, null)
        canvas.drawBitmap(twoTiles[1], srcRect, dstRectRight, null)

        return resultBitmap
    }

    private fun initMapClicks(
        map: MapLibreMap,
        photoLayer: SymbolLayer,
        clusterLayer: SymbolLayer,
    ) {
        map.addOnMapClickListener { latLng ->
            val clickedFeature =
                map
                    .queryRenderedFeatures(
                        map.projection.toScreenLocation(latLng),
                        photoLayer.id,
                        clusterLayer.id,
                    )
                    .firstOrNull()
                    ?: return@addOnMapClickListener false

            log.debug {
                "initMapClicks:onMapClick(): clicked_feature:" +
                        "\nfeature=$clickedFeature"
            }

            if (clickedFeature.hasProperty("UID")) {
                viewModel.onPhotoClicked(
                    uid = clickedFeature.getStringProperty("UID")
                )
            } else {
                viewModel.onClusterClicked(
                    latNorth = clickedFeature.getNumberProperty("LatNorth").toDouble(),
                    lngEast = clickedFeature.getNumberProperty("LngEast").toDouble(),
                    latSouth = clickedFeature.getNumberProperty("LatSouth").toDouble(),
                    lngWest = clickedFeature.getNumberProperty("LngWest").toDouble(),
                )
            }

            return@addOnMapClickListener true
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            MapViewModel.Event.ShowFloatingLoadingFailedError ->
                Toast.makeText(
                    this,
                    R.string.failed_to_load_data,
                    Toast.LENGTH_SHORT
                ).show()

            is MapViewModel.Event.OpenViewer -> {
                startActivity(
                    Intent(this, MediaViewerActivity::class.java)
                        .putExtras(
                            MediaViewerActivity.getBundle(
                                mediaIndex = 0,
                                repositoryParams = event.repositoryParams,
                            )
                        )
                )
            }

            is MapViewModel.Event.OpenCluster -> {
                startActivity(
                    Intent(this, GallerySingleRepositoryActivity::class.java)
                        .putExtras(
                            GallerySingleRepositoryActivity.getBundle(
                                repositoryParams = event.repositoryParams,
                                title = getString(R.string.in_this_place),
                            )
                        )
                )
            }
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
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

    private fun LatLngBounds.toRectF(map: MapLibreMap): RectF {
        val projection = map.projection
        val ne = projection.toScreenLocation(northEast)
        val sw = projection.toScreenLocation(southWest)
        return RectF(sw.x, ne.y, ne.x, sw.y)
    }

    private fun SymbolLayer.invalidate() {
        setProperties(iconSize(iconSize.value!! + 0.0001f))
    }

    private fun Point.toLatLng(): LatLng =
        LatLng(latitude(), longitude())

    private fun BoundingBox.toLatLngBounds(): LatLngBounds =
        LatLngBounds.Builder()
            .include(southwest().toLatLng())
            .include(northeast().toLatLng())
            .build()

    private companion object {
        private const val SOURCE_ID = "pp-clustered-photos"
    }
}
