package ua.com.radiokot.photoprism.features.viewer.view

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

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

    @Suppress("DEPRECATION")
    private val callbackPeriodMs =
        (1000f / windowManager.defaultDisplay.refreshRate).roundToLong()
    private var callbackTimer: Disposable? = null
    private var lastCallbackMs = -1L

    var isRunning: Boolean = false
        private set

    private var speedXRadS = 0f
    private var speedYRadS = 0f
    private var speedZRadS = 0f

    fun start() {
        if (isRunning || gyroscope == null) {
            return
        }

        isRunning = true
        speedXRadS = 0f
        speedYRadS = 0f
        speedZRadS = 0f

        sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_GAME,
        )

        callbackTimer = createCallbackTimer().subscribe()
    }

    @Suppress("DEPRECATION", "UnnecessaryVariable")
    private fun createCallbackTimer(

    ) = Observable
        .interval(callbackPeriodMs, TimeUnit.MILLISECONDS)
        .map { System.currentTimeMillis() }
        .doOnNext { nowMs ->

            if (lastCallbackMs == -1L
                || (speedXRadS == 0f && speedYRadS == 0f && speedZRadS == 0f)
            ) {
                lastCallbackMs = nowMs
                return@doOnNext
            }

            val dt = nowMs - lastCallbackMs
            lastCallbackMs = nowMs

            val dx = speedXRadS * dt / 1000f
            val dy = speedYRadS * dt / 1000f
            val dz = speedZRadS * dt / 1000f

            val deltaRoll = dz
            val deltaPitch: Float
            val deltaYaw: Float
            val displayRotation = windowManager.defaultDisplay.rotation

            when (displayRotation) {
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
                displayRotation = displayRotation,
            )
        }

    fun stop() {
        callbackTimer?.dispose()
        sensorManager.unregisterListener(this)
        isRunning = false
    }

    override fun onSensorChanged(
        event: SensorEvent,
    ) {
        speedXRadS = event.values[0]
        speedYRadS = event.values[1]
        speedZRadS = event.values[2]
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
        displayRotation: Int,
    )
}
