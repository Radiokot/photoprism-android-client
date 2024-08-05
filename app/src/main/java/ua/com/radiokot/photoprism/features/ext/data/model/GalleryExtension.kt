package ua.com.radiokot.photoprism.features.ext.data.model

import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags

enum class GalleryExtension(
    val feature: FeatureFlags.Feature,
) {
    MEMORIES(FeatureFlags.Feature.MEMORIES_EXTENSION),
    TEST(FeatureFlags.Feature.TEST_EXTENSION),
    PHOTO_FRAME_WIDGET(FeatureFlags.Feature.PHOTO_FRAME_WIDGET),
    ;
}
