package ua.com.radiokot.photoprism.features.env.data.storage

import org.koin.core.Koin
import org.koin.core.qualifier.named
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.features.env.data.model.EnvSession

class KoinScopeEnvSessionHolder(
    private val koin: Koin,
) : EnvSessionHolder {
    override fun set(session: EnvSession) {
        with(koin) {
            getScopeOrNull(DI_SCOPE_SESSION)?.close()
            createScope(
                scopeId = DI_SCOPE_SESSION,
                qualifier = named<EnvSession>(),
                source = session
            )
        }
    }

    override val isSet: Boolean
        get() = with(koin) {
            return getScopeOrNull(DI_SCOPE_SESSION)?.isNotClosed() == true
        }
}