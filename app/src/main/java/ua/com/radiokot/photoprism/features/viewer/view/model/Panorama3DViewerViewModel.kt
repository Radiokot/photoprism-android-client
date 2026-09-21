package ua.com.radiokot.photoprism.features.viewer.view.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.viewer.view.GyroscopeRotationTracker

class Panorama3DViewerViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val log = kLogger("Panorama3DViewerVM")
    private val gyroscopeRotationTracker = GyroscopeRotationTracker(
        context = application,
        onRotation = ::onGyroRotation,
    )

    val cameraOrientation = Quaternion()

    val cameraFovDegrees: Observable<Float>
        field = BehaviorSubject.createDefault(75f)
    private val minFovDegrees = 30f
    private val maxFovDegrees = 90f

    val isFullScreen: Observable<Boolean>
        field = BehaviorSubject.createDefault(false)

    val isSensorEnabled: Observable<Boolean>
        field = BehaviorSubject.createDefault(true)

    init {
        isSensorEnabled.subscribe { isSensorEnabled ->
            if (isSensorEnabled) {
                gyroscopeRotationTracker.start()
            } else {
                gyroscopeRotationTracker.stop()
            }
        }.autoDispose(this)
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

    private fun onGyroRotation(
        deltaPitchRad: Float,
        deltaRollRad: Float,
        deltaYawRad: Float,
    ) {
        cameraOrientation.rotateAroundItself(
            deltaPitch = deltaPitchRad,
            deltaYaw = deltaYawRad,
            deltaRoll = deltaRollRad,
        )
    }
}
