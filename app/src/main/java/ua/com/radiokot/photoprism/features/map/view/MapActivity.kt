package ua.com.radiokot.photoprism.features.map.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.coalesce
import org.maplibre.android.style.expressions.Expression.concat
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.image
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.not
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
    private val thumbnailSizePx = 200
    private val roundedCornersTransformation: Transformation by lazy {
        ImageTransformations.roundedCorners(
            cornerRadiusDp = 8,
            context = this,
        )
    }
    private val clusterPhotoCountPaint: Paint by lazy {
        Paint().apply {
            color = MaterialColors.getColor(
                this@MapActivity,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                Color.RED
            )
            textSize = thumbnailSizePx * 0.18f
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }
    private val clusterPhotoCountBackgroundPaint: Paint by lazy {
        Paint().apply {
            color = MaterialColors.getColor(
                this@MapActivity,
                com.google.android.material.R.attr.colorPrimaryContainer,
                Color.RED
            )
            isAntiAlias = true
            style = Paint.Style.FILL
        }
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

    private fun initMap() = view.map.getMapAsync { map ->
        map.setMaxZoomPreference(20.0)
        map.setStyle("https://cdn.photoprism.app/maps/default.json")
        map.cameraPosition = CameraPosition.DEFAULT

        map.getStyle { style ->
            viewModel
                .source
                .observe(this@MapActivity) { source ->
                    // TODO: Fix crash on config change.
                    style.addSource(source)
                    // TODO: Set camera position corresponding to the source bounding box.
                }

            style.addImage(
                "placeholder",
                ContextCompat.getDrawable(this, R.drawable.image_placeholder_circle)!!
                    .toBitmap(
                        width = thumbnailSizePx / 2,
                        height = thumbnailSizePx / 2,
                    )
            )

            val clusterLayer =
                SymbolLayer("pp-clusters", MapViewModel.SOURCE_ID)
                    .withProperties(
                        PropertyFactory.iconImage(
                            coalesce(
                                image(
                                    concat(
                                        get("point_count"),
                                        literal(":"),
                                        get("Hashes")
                                    )
                                ),
                                image(Expression.literal("placeholder"))
                            )
                        ),
                        PropertyFactory.iconSize(1f),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true)
                    )
                    .withFilter(has("point_count"))
                    .also(style::addLayer)

            val photoLayer =
                SymbolLayer("pp-photos", MapViewModel.SOURCE_ID)
                    .withProperties(
                        PropertyFactory.iconImage(
                            coalesce(
                                image(get("Hash")),
                                image(Expression.literal("placeholder"))
                            )
                        ),
                        PropertyFactory.iconSize(1f),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true)
                    )
                    .withFilter(not(has("point_count")))
                    .also(style::addLayer)

            val markerUpdateEvents = PublishSubject.create<Unit>()
            view.map.addOnDidFinishRenderingFrameListener { fully, _, _ ->
                if (fully) {
                    markerUpdateEvents.onNext(Unit)
                }
            }

            markerUpdateEvents
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
                .map { visibleFeatures ->
                    visibleFeatures
                        .mapTo(mutableSetOf()) { feature ->
                            if (feature.hasProperty("Hashes"))
                                feature.getNumberProperty("point_count").toString() +
                                        ":" +
                                        feature.getStringProperty("Hashes")
                            else
                                feature.getStringProperty("Hash")
                        }
                        .filter { style.getImage(it) == null }
                }
                .subscribe { thumbnailsToLoad ->
                    thumbnailsToLoad.forEach { thumbnailId ->
                        val isCluster = thumbnailId.contains(':')

                        val getThumbnailBitmap =
                            if (isCluster)
                                getClusterThumbnailBitmap(thumbnailId)
                            else
                                getPhotoThumbnailBitmap(thumbnailId)

                        getThumbnailBitmap
                            .subscribeBy { thumbnailBitmap ->
                                log.debug { "thumbnail loaded $thumbnailId" }
                                style.addImage(thumbnailId, thumbnailBitmap)
                                if (isCluster) {
                                    clusterLayer.invalidate()
                                } else {
                                    photoLayer.invalidate()
                                }
                            }
                            .autoDispose(this)
                    }
                }
                .autoDispose(this)

            log.debug {
                "initMap(): style_initialized"
            }
        }

        log.debug {
            "initMap(): map_initialized"
        }
    }

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
            .transform(roundedCornersTransformation)
            .intoSingle()

    private fun getClusterThumbnailBitmap(
        thumbnailId: String
    ): Single<Bitmap> {
        val thumbnailHashes =
            thumbnailId
                .substringAfter(':')
                .split(',')
                .filter(String::isNotEmpty)
        val photoCount =
            thumbnailId
                .substringBefore(':')
                .toInt()

        val composeTiles =
            if (thumbnailHashes.size >= 4) {
                val tileSize = thumbnailSizePx / 2
                thumbnailHashes
                    .take(4)
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
                    .take(2)
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
            .map {
                addPhotoCount(
                    source = it,
                    count = photoCount,
                )
            }
            .map(roundedCornersTransformation::transform)
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

    private fun addPhotoCount(
        source: Bitmap,
        count: Int,
    ): Bitmap {
        val canvas = Canvas(source)

        val text =
            if (count >= 1000)
                "${count / 1000}k"
            else
                count.toString()

        val textPaddingHorizontal = source.width * 0.06f
        val textWidth = clusterPhotoCountPaint.measureText(text) + textPaddingHorizontal * 2f
        val textPaddingVertical = source.width * 0.02f
        val textHeight =
            clusterPhotoCountPaint.fontMetrics.bottom -
                    clusterPhotoCountPaint.fontMetrics.top +
                    textPaddingVertical * 2f
        val textDescent =
            clusterPhotoCountPaint.fontMetrics.descent

        val textLeft = (canvas.width - textWidth) / 2f
        val textTop = (canvas.height - textHeight) / 2f
        val textBackgroundRect =
            RectF(
                textLeft,
                textTop,
                textLeft + textWidth,
                textTop + textHeight
            )

        val textBackgroundCornerRadius = source.width * 0.05f
        canvas.drawRoundRect(
            textBackgroundRect,
            textBackgroundCornerRadius,
            textBackgroundCornerRadius,
            clusterPhotoCountBackgroundPaint
        )

        canvas.drawText(
            text,
            textBackgroundRect.left + textPaddingHorizontal,
            textBackgroundRect.bottom - textDescent - textPaddingVertical,
            clusterPhotoCountPaint
        )

        return source
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
        setProperties(PropertyFactory.iconSize(iconSize.value!! + 0.0001f))
    }
}
