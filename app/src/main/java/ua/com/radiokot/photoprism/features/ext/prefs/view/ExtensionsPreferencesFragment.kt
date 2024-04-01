package ua.com.radiokot.photoprism.features.ext.prefs.view

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createFragmentScope
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.featureflags.extension.hasMemoriesExtension
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import ua.com.radiokot.photoprism.features.ext.key.activation.view.KeyActivationActivity
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesPreferences
import ua.com.radiokot.photoprism.features.ext.memories.view.MemoriesNotificationsManager
import ua.com.radiokot.photoprism.features.prefs.extension.requirePreference

// When renaming or moving this fragment,
// make sure to manually update the preference "fragment" value in preferences.xml.
class ExtensionsPreferencesFragment :
    PreferenceFragmentCompat(),
    AndroidScopeComponent {

    override val scope: Scope by lazy {
        getKoin().getScope(DI_SCOPE_SESSION)
            .apply { linkTo(createFragmentScope()) }
    }

    private val featureFlags: FeatureFlags by inject()
    private val memoriesNotificationsManager: MemoriesNotificationsManager by inject()
    private val memoriesPreferences: MemoriesPreferences by inject()
    private val galleryExtensionsStateRepository: GalleryExtensionsStateRepository by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.extensions_preferences, rootKey)

        initGeneralPreferences()

        subscribeToExtensionsState()
    }

    private fun initGeneralPreferences() = with(preferenceScreen) {
        with(requirePreference(R.string.pk_ext_enter_key)) {
            setOnPreferenceClickListener {
                startActivity(Intent(requireActivity(), KeyActivationActivity::class.java))
                true
            }
        }
    }

    private fun subscribeToExtensionsState() {
        galleryExtensionsStateRepository.state
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                initSummary()
                initExtensionSpecificPreferences()
            }
            .autoDispose(this)
    }

    private fun initSummary() = with(preferenceScreen) {
        with(requirePreference(R.string.pk_ext_summary)) {
            val activatedExtensionsCount =
                galleryExtensionsStateRepository.currentState.activatedExtensions.size
            val primarySubject = galleryExtensionsStateRepository.currentState.primarySubject

            if (activatedExtensionsCount > 0 && primarySubject != null) {
                isVisible = true
                summary = getString(
                    R.string.template_extensions_preferences_activated_extensions_for,
                    requireContext().resources.getQuantityString(
                        R.plurals.activated_extensions,
                        activatedExtensionsCount,
                        activatedExtensionsCount,
                    ),
                    primarySubject,
                )
            } else {
                isVisible = false
            }
        }
    }

    private fun initExtensionSpecificPreferences() = with(preferenceScreen) {
        with(requirePreference(R.string.pk_ext_memories)) {
            isVisible = featureFlags.hasMemoriesExtension
        }

        if (featureFlags.hasMemoriesExtension) {
            with(requirePreference(R.string.pk_ext_memories_notifications)) {
                setOnPreferenceChangeListener { _, newValue ->
                    if (!memoriesNotificationsManager.areNotificationsEnabled
                        || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    ) {
                        // Open system settings if the notifications are disabled globally
                        // or the notification channel settings are available (since Oreo).
                        startActivity(memoriesNotificationsManager.getSystemSettingsIntent())
                    } else {
                        memoriesPreferences.areNotificationsEnabled = newValue == true
                        updateMemoriesNotificationsChecked()
                    }
                    false
                }

                updateMemoriesNotificationsChecked()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updateMemoriesNotificationsChecked()
    }

    private fun updateMemoriesNotificationsChecked(
    ) = with(preferenceScreen.requirePreference(R.string.pk_ext_memories_notifications)) {
        this as SwitchPreferenceCompat
        isChecked = memoriesNotificationsManager.areMemoriesNotificationsEnabled
    }

    companion object {
        val EXTENSIONS_WITH_PREFERENCES = setOf(
            GalleryExtension.MEMORIES,
        )
    }
}
