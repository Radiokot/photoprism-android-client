package ua.com.radiokot.photoprism.env.data.storage

import org.koin.core.Koin
import org.koin.core.qualifier.named
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.kLogger

class KoinScopeEnvSessionHolder(
    private val koin: Koin,
) : EnvSessionHolder {
    private val log = kLogger("KoinScopeEnvSessionHolder")

    override fun set(session: EnvSession) {
        with(koin) {
            closeExistingScope()

            createScope(
                scopeId = DI_SCOPE_SESSION,
                qualifier = named<EnvSession>(),
                source = session
            )

            log.debug {
                "set(): created_new_scope:" +
                        "\nscopeId=$DI_SCOPE_SESSION"
            }
        }
    }

    override fun clear() {
        closeExistingScope()
    }

    private fun closeExistingScope() {
        with(koin) {
            getScopeOrNull(DI_SCOPE_SESSION)?.close()?.also {
                log.debug { "closeExistingScope(): scope_closed" }
            }
        }
    }

    override val isSet: Boolean
        get() = with(koin) {
            return getScopeOrNull(DI_SCOPE_SESSION)?.isNotClosed() == true
        }
}