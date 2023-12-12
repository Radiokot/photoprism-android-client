package ua.com.radiokot.photoprism.features.viewer.slideshow.data.model

enum class SlideshowSpeed(
    val presentationDurationMs: Int,
) {
    SLOW(4000),
    SLOWER(3200),
    NORMAL(2500),
    FASTER(1800),
    FAST(1000),
    ;
}
