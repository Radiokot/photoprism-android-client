package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

class GyroscopeRotationTracker(
    context: Context,
    private val onRotation: OnGyroRotation,
) : SensorEventListener {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var lastEventNs: Long = 0L
    private val noiseThreshold = 0.005f

    var isRunning: Boolean = false
        private set

    fun start() {
        if (gyroscope == null) {
            return
        }

        lastEventNs = 0L
        isRunning = true

        sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_GAME,
        )
    }

    fun stop() {
        isRunning = false
        sensorManager.unregisterListener(this)
    }

    @Suppress("UnnecessaryVariable", "DEPRECATION")
    override fun onSensorChanged(
        event: SensorEvent,
    ) {
        if (lastEventNs == 0L) {
            lastEventNs = event.timestamp
            return
        }

        // Radians per second.
        var (gyroX, gyroY, gyroZ) = event.values

        if (abs(gyroX) < noiseThreshold) {
            gyroX = 0f
        }

        if (abs(gyroY) < noiseThreshold) {
            gyroY = 0f
        }

        if (abs(gyroZ) < noiseThreshold) {
            gyroZ = 0f
        }

        val dt = (event.timestamp - lastEventNs) / 1000000000f
        lastEventNs = event.timestamp

        val dx = gyroX * dt
        val dy = gyroY * dt
        val dz = gyroZ * dt

        val deltaRoll = dz
        val deltaPitch: Float
        val deltaYaw: Float

        when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> {
                deltaPitch = -dy
                deltaYaw = dx
            }

            Surface.ROTATION_270 -> {
                deltaPitch = dy
                deltaYaw = -dx
            }

            else -> {
                // Yes.
                deltaPitch = dx
                deltaYaw = dy
            }
        }

        onRotation(
            deltaPitchRad = deltaPitch,
            deltaRollRad = deltaRoll,
            deltaYawRad = deltaYaw,
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Doesn't matter.
    }
}

fun interface OnGyroRotation {
    operator fun invoke(
        deltaPitchRad: Float,
        deltaRollRad: Float,
        deltaYawRad: Float,
    )
}
