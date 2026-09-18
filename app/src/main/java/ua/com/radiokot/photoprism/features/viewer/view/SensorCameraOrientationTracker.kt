package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager

class SensorCameraOrientationTracker(
    context: Context,
    private val onOrientationUpdated: (
        pitchDegrees: Float,
        yawDegrees: Float,
    ) -> Unit,
) : SensorEventListener {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val compensatedRotationMatrix = FloatArray(9)
    private val orientationRadians = FloatArray(3)

    // The lower the factor, the greater the smoothing.
    private val smoothingFactor = 0.3f
    private var smoothedPitchDegrees = 0f
    private var smoothedYawDegrees = 0f
    private var isInitialized = false

    fun start() {
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME,
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isInitialized = false
    }

    @Suppress("DEPRECATION")
    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        compensateDeviceRotation(
            deviceRotation = windowManager.defaultDisplay.rotation,
        )
        SensorManager.getOrientation(compensatedRotationMatrix, orientationRadians)

        val yawDegrees = 57.29578f * orientationRadians[0]
        val pitchDegrees = 57.29578f * orientationRadians[1]

        if (!isInitialized) {
            smoothedPitchDegrees = pitchDegrees
            smoothedYawDegrees = yawDegrees
            isInitialized = true
        } else {
            smoothedPitchDegrees +=
                smoothingFactor * deltaAngle(smoothedPitchDegrees, pitchDegrees)

            // Keep yaw positive, otherwise unwanted spin happens around 0.
            smoothedYawDegrees +=
                smoothingFactor * deltaAngle(smoothedYawDegrees, yawDegrees) + 360f
            smoothedYawDegrees %= 360
        }

        onOrientationUpdated(
            smoothedPitchDegrees,
            smoothedYawDegrees,
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Doesn't matter.
    }

    private fun compensateDeviceRotation(
        deviceRotation: Int,
    ) = when (deviceRotation) {
        Surface.ROTATION_90 ->
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_Z,
                SensorManager.AXIS_MINUS_X,
                compensatedRotationMatrix
            )

        Surface.ROTATION_180 ->
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_MINUS_X,
                SensorManager.AXIS_MINUS_Z,
                compensatedRotationMatrix
            )

        Surface.ROTATION_270 ->
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_MINUS_Z,
                SensorManager.AXIS_X,
                compensatedRotationMatrix
            )

        else ->
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                compensatedRotationMatrix
            )
    }

    private fun deltaAngle(
        current: Float,
        target: Float,
    ): Float {
        var delta = (target - current) % 360f
        // Avoid spinning in the wrong direction.
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }
}
