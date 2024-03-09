package ua.com.radiokot.photoprism.features.ext.prefs.view

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createFragmentScope
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.featureflags.extension.hasMemoriesExtension
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.prefs.extension.requirePreference

// When renaming or moving this fragment,
// make sure to manually update the preference "fragment" value in preferences.xml.
class ExtensionPreferencesFragment :
    PreferenceFragmentCompat(),
    AndroidScopeComponent {

    override val scope: Scope by lazy {
        getKoin().getScope(DI_SCOPE_SESSION)
            .apply { linkTo(createFragmentScope()) }
    }

    private val featureFlags: FeatureFlags by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.extension_preferences, rootKey)

        initPreferences()
    }

    private fun initPreferences() = with(preferenceScreen) {
        with(requirePreference(R.string.pk_ext_memories)) {
            isVisible = featureFlags.hasMemoriesExtension
        }
    }
}
