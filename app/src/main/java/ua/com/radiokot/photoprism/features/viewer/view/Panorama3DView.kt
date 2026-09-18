package ua.com.radiokot.photoprism.features.viewer.view

interface Panorama3DView {
    /**
     * 0 to look at the image center.
     * Positive to look up, negative to look down, up to 90 degrees.
     */
    var pitchDegrees: Float

    /**
     * 0 to look at the image center.
     */
    var yawDegrees: Float

    var rollDegrees: Float

    /**
     * The smaller the field of view, the greater the zoom.
     */
    var fovDegrees: Float
}
