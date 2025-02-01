package ua.com.radiokot.photoprism.features.ext.key.renewal

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.ext.key.renewal.view.model.KeyRenewalViewModel

val keyRenewalFeatureModule = module {

    scope<EnvSession> {
        viewModelOf(::KeyRenewalViewModel)
    }
}
