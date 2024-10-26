package ua.com.radiokot.photoprism.features.ext.prefs.view

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createFragmentScope
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.featureflags.extension.hasExtensionStore
import ua.com.radiokot.photoprism.featureflags.extension.hasMemoriesExtension
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import ua.com.radiokot.photoprism.features.ext.key.activation.view.KeyActivationActivity
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesPreferences
import ua.com.radiokot.photoprism.features.ext.memories.view.MemoriesNotificationsManager
import ua.com.radiokot.photoprism.features.ext.store.view.GalleryExtensionStoreActivity
import ua.com.radiokot.photoprism.features.prefs.extension.requirePreference


// When renaming or moving this fragment,
// make sure to manually update the preference "fragment" value in preferences.xml.
class GalleryExtensionPreferencesFragment :
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
    }

    override fun setPreferenceScreen(preferenceScreen: PreferenceScreen) {
        initGeneralPreferences(preferenceScreen)
        super.setPreferenceScreen(preferenceScreen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToExtensionsState()
    }

    private fun initGeneralPreferences(preferenceScreen: PreferenceScreen) =
        with(preferenceScreen) {
            with(requirePreference(R.string.pk_ext_store)) {
                isVisible = featureFlags.hasExtensionStore
                setOnPreferenceClickListener {
                    startActivity(
                        Intent(
                            requireActivity(),
                            GalleryExtensionStoreActivity::class.java
                        )
                    )
                    true
                }
            }

            with(requirePreference(R.string.pk_ext_activate_key)) {
                setOnPreferenceClickListener {
                    startActivity(Intent(requireActivity(), KeyActivationActivity::class.java))
                    true
                }
            }

            with(requirePreference(R.string.pk_ext_activated_keys)) {
                setOnPreferenceClickListener {
                    showActivatedKeysDialog()
                    true
                }
            }
        }

    private fun subscribeToExtensionsState() {
        galleryExtensionsStateRepository.state
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this) {
                initSummary()
                initExtensionSpecificPreferences()
            }
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

    private fun showActivatedKeysDialog() {
        val keysString = galleryExtensionsStateRepository.currentState
            .activatedKeys
            .joinToString("\n\n")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.activated_keys)
            .setMessage(keysString)
            .setNeutralButton(R.string.share) { _, _ ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, keysString)
                }
                runCatching {
                    startActivity(Intent.createChooser(intent, getString(R.string.activated_keys)))
                }
            }
            .setPositiveButton(R.string.ok, null)
            .show()
            .apply {
                with(findViewById<TextView>(android.R.id.message)!!) {
                    setTypeface(Typeface.MONOSPACE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextIsSelectable(true)
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
