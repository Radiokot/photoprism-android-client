package ua.com.radiokot.photoprism.features.ext.key.activation

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.ext.galleryExtensionsFeatureModule
import ua.com.radiokot.photoprism.features.ext.key.activation.logic.ActivateParsedKeyUseCase
import ua.com.radiokot.photoprism.features.ext.key.activation.logic.ParseEnteredKeyUseCase
import ua.com.radiokot.photoprism.features.ext.key.activation.view.model.KeyActivationViewModel
import ua.com.radiokot.photoprism.features.ext.key.logic.AndroidHardwareIdentifier
import ua.com.radiokot.photoprism.features.ext.key.logic.CreateExtensionsHelpEmailUseCase
import ua.com.radiokot.photoprism.features.ext.key.logic.HardwareIdentifier

val keyActivationFeatureModule = module {
    includes(galleryExtensionsFeatureModule)

    single {
        AndroidHardwareIdentifier
    } bind HardwareIdentifier::class

    singleOf(::ParseEnteredKeyUseCase)
    singleOf(::ActivateParsedKeyUseCase)

    single {
        CreateExtensionsHelpEmailUseCase(
            helpEmailAddress = getProperty("helpEmailAddress"),
            hardwareIdentifier = get(),
        )
    } bind CreateExtensionsHelpEmailUseCase::class

    scope<EnvSession> {
        viewModelOf(::KeyActivationViewModel)
    }
}
