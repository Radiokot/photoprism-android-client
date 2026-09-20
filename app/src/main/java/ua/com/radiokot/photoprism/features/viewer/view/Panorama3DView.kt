package ua.com.radiokot.photoprism.features.viewer.view

import android.os.Bundle

interface Panorama3DView {

    /**
     * The smaller the field of view, the greater the zoom.
     */
    var fovDegrees: Float

    /**
     * @param deltaX pointer (finger) move distance in popugays
     * @param deltaY pointer (finger) move distance in popugays
     */
    fun rotateByPointer(
        deltaX: Float,
        deltaY: Float,
    )

    /**
     * @param deltaPitch in radians
     * @param deltaYaw in radians
     * @param deltaRoll in radians
     */
    fun rotateByGyro(
        deltaPitch: Float,
        deltaYaw: Float,
        deltaRoll: Float,
    )

    // Since the view is created programmatically,
    // its state is saved and restored manually.

    fun saveState(outState: Bundle)

    fun restoreState(savedInstanceState: Bundle)
}
