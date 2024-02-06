package ua.com.radiokot.photoprism.features.featureflags.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.features.featureflags.logic.DebugOnlyFeatureFlags
import ua.com.radiokot.photoprism.features.featureflags.logic.FeatureFlags

val featureFlagsModule = module {
    singleOf(::DebugOnlyFeatureFlags) bind FeatureFlags::class
}
