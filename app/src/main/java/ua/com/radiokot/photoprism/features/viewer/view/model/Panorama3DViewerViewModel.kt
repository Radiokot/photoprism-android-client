package ua.com.radiokot.photoprism.features.viewer.view.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.viewer.view.GyroscopeRotationTracker
import java.util.concurrent.TimeUnit

class Panorama3DViewerViewModel(
    parameters: Parameters,
    application: Application,
) : AndroidViewModel(application) {

    private val log = kLogger("Panorama3DViewerVM")
    private val gyroscopeRotationTracker = GyroscopeRotationTracker(
        context = application,
        onRotation = ::onGyroRotation,
    )
    private var gyroscopeCounteredDisplayRotation = -1

    val cameraOrientation = Quaternion().apply {
        rotateAroundAxis(
            //                                      π/180
            angle = -parameters.initialYawDegrees * 0.017453292f,
            axisX = 0f,
            axisY = 1f,
            axisZ = 0f,
        )
    }

    val cameraFovDegrees: Observable<Float>
        field = BehaviorSubject.createDefault(75f)
    private val minFovDegrees = 30f
    private val maxFovDegrees = 90f

    val isFullScreen: Observable<Boolean>
        field = BehaviorSubject.createDefault(false)

    val isSensorEnabled: Observable<Boolean>
        field = BehaviorSubject.createDefault(true)

    private var screenDeactivationDisposable: Disposable? = null
    private val isScreenActive = BehaviorSubject.createDefault(false)

    init {
        Observable
            .combineLatest(
                isSensorEnabled,
                isScreenActive,
            ) { isSensorEnabled, isScreenActive ->
                isSensorEnabled && isScreenActive
            }
            .doOnNext { toStartGyro ->
                if (toStartGyro) {
                    startGyro()
                } else {
                    stopGyro()
                }
            }
            .doOnDispose(::stopGyro)
            .subscribe()
            .autoDispose(this)
    }

    private fun startGyro() {
        if (gyroscopeRotationTracker.isRunning) {
            return
        }

        log.debug {
            "startGyro(): starting_gyroscope_rotation_tracker"
        }

        gyroscopeCounteredDisplayRotation = -1
        gyroscopeRotationTracker.start()
    }

    private fun stopGyro() {
        if (!gyroscopeRotationTracker.isRunning) {
            return
        }

        log.debug {
            "stopGyro(): stopping_gyroscope_rotation_tracker"
        }

        gyroscopeRotationTracker.stop()
    }

    fun onSensorToggleClicked() {
        val toEnable = !isSensorEnabled.value!!

        log.debug {
            "onSensorToggleClicked(): toggling_sensor:" +
                    "\ntoEnable=$toEnable"
        }

        isSensorEnabled.onNext(toEnable)
    }

    fun onFullScreenToggledBySystem(
        isFullScreen: Boolean,
    ) {
        if (isFullScreen == this.isFullScreen.value) {
            return
        }

        log.debug {
            "onFullScreenToggledBySystem(): system_toggled_full_screen:" +
                    "\nisFullScreen=$isFullScreen"
        }

        this.isFullScreen.onNext(isFullScreen)
    }

    fun onPanoramaClicked() {
        val toFullScreen = !isFullScreen.value!!

        log.debug {
            "onPanoramaClicked(): toggling_full_screen:" +
                    "\ntoFullScreen=$toFullScreen"
        }

        isFullScreen.onNext(toFullScreen)
    }

    fun onScaleGesture(
        factor: Float,
    ) {
        val newFovDegrees =
            (cameraFovDegrees.value!! / factor).coerceIn(minFovDegrees, maxFovDegrees)

        if (newFovDegrees == cameraFovDegrees.value) {
            return
        }

        cameraFovDegrees.onNext(newFovDegrees)
    }

    fun onDragGesture(
        deltaX: Float,
        deltaY: Float,
    ) {
        //           sensitivity scaled by FOV
        val factor = 0.004f * cameraFovDegrees.value!! / maxFovDegrees
        val scaledDeltaX = deltaX * factor
        val scaledDeltaY = deltaY * factor

        cameraOrientation.rotateAroundAxis(
            angle = -scaledDeltaX,
            axisX = 0f,
            axisY = 1f,
            axisZ = 0f,
        )
        cameraOrientation.rotateAroundItself(
            deltaPitch = -scaledDeltaY,
            deltaYaw = 0f,
            deltaRoll = 0f,
        )
    }

    fun onScreenResumed() {
        screenDeactivationDisposable?.dispose()
        isScreenActive.onNext(true)
    }

    fun onScreenPaused() {
        log.debug {
            "onScreenPaused(): scheduling_scren_deactivation"
        }

        screenDeactivationDisposable?.dispose()
        screenDeactivationDisposable =
            Observable
                .timer(2, TimeUnit.SECONDS)
                .doOnDispose {
                    log.debug {
                        "onScreenPaused(): scren_deactivation_cancelled"
                    }
                }
                .subscribe { isScreenActive.onNext(false) }
                .autoDispose(this)
    }

    private fun onGyroRotation(
        deltaPitchRad: Float,
        deltaRollRad: Float,
        deltaYawRad: Float,
        displayRotation: Int,
    ) {
        // When the display is rotated with gyro tracking enabled,
        // activity rotation must be countered
        // because all the needed roll is already applied.

        val counterDisplayRotation =
            if (gyroscopeCounteredDisplayRotation != -1)
                (gyroscopeCounteredDisplayRotation - displayRotation) * 1.5707964f // π/2
            else
                0f

        cameraOrientation.rotateAroundItself(
            deltaPitch = deltaPitchRad,
            deltaYaw = deltaYawRad,
            deltaRoll = deltaRollRad + counterDisplayRotation,
        )

        gyroscopeCounteredDisplayRotation = displayRotation
    }

    class Parameters(
        val initialYawDegrees: Float,
    )
}
