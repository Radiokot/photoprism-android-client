package ua.com.radiokot.photoprism.features.ext.key.input.di

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.ext.di.galleryExtensionsFeatureModule
import ua.com.radiokot.photoprism.features.ext.key.input.logic.ParseEnteredKeyUseCase
import ua.com.radiokot.photoprism.features.ext.key.input.view.model.KeyInputViewModel

val keyInputFeatureModule = module {
    includes(galleryExtensionsFeatureModule)

    singleOf(ParseEnteredKeyUseCase::Factory)

    scope<EnvSession> {
        viewModelOf(::KeyInputViewModel)
    }
}
