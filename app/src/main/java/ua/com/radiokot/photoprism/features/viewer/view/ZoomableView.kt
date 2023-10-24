package ua.com.radiokot.photoprism.features.viewer.view

interface ZoomableView {
    val isZoomed: Boolean

    /**
     * Check if pan (moving a view frame) possible in a certain direction.
     * Dragging right causes pan left and vice versa.
     *
     * @param direction Negative to check pan left, positive to check pan right.
     * @return **true** if the pan is possible in the specified direction, **false** otherwise.
     */
    fun canPanHorizontally(direction: Int): Boolean
}
