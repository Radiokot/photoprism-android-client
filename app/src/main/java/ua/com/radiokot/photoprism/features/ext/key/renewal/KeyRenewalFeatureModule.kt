package ua.com.radiokot.photoprism.features.ext.key.renewal

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.ext.key.renewal.logic.RenewEnteredKeyUseCase
import ua.com.radiokot.photoprism.features.ext.key.renewal.view.model.KeyRenewalViewModel

val keyRenewalFeatureModule = module {
    single {
        RenewEnteredKeyUseCase(
            issuerId = getProperty<String>("offlineLicenseKeySvcIssuerId")
                .checkNotNull { "Missing offline license key service issuer ID" },
            offlineLicenseKeyService = get(),
            hardwareIdentifier = get(),
        )
    } bind RenewEnteredKeyUseCase::class

    viewModelOf(::KeyRenewalViewModel)
}
