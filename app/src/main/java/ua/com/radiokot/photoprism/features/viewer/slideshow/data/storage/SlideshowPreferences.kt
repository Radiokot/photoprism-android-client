package ua.com.radiokot.photoprism.features.viewer.slideshow.data.storage

import ua.com.radiokot.photoprism.features.viewer.slideshow.data.model.SlideshowSpeed

interface SlideshowPreferences {
    var isGuideAccepted: Boolean
    var speed: SlideshowSpeed
}
