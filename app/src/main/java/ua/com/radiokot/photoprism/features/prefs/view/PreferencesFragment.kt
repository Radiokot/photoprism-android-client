package ua.com.radiokot.photoprism.features.prefs.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createFragmentScope
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.features.gallery.logic.ExportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.util.CustomTabsHelper

class PreferencesFragment : PreferenceFragmentCompat(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createFragmentScope().apply {
            linkTo(getScope(DI_SCOPE_SESSION))
        }
    }

    private val log = kLogger("PreferencesFragment")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        initCustomTabs()
        initPreferences()
    }

    private fun initCustomTabs() {
        CustomTabsHelper.safelyConnectAndInitialize(requireContext())
    }

    private fun initPreferences() = preferenceScreen.apply {
        with(requirePreference<Preference>(R.string.pk_library_api_url)) {
            summary = get<EnvSession>().envConnectionParams.apiUrl
        }

        with(requirePreference<Preference>(R.string.pk_export_bookmarks)) {
            setOnPreferenceClickListener {
                exportBookmarks()
                true
            }
        }

        with(requirePreference<Preference>(R.string.pk_app_version)) {
            summary = BuildConfig.VERSION_NAME
        }

        with(requirePreference<Preference>(R.string.pk_report_issue)) {
            setOnPreferenceClickListener {
                openIssueReport()
                true
            }
        }
    }

    private fun exportBookmarks() {
        log.debug { "exportBookmarks(): begin_export" }

        get<ExportSearchBookmarksUseCase>()
            .perform()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { intent ->
                    log.debug {
                        "exportBookmarks(): successfully_exported:" +
                                "\nintent=$intent"
                    }

                    startActivity(
                        Intent.createChooser(
                            intent,
                            getString(R.string.save_exported_file)
                        )
                    )
                },
                onError = { error ->
                    log.error(error) {
                        "exportBookmarks(): error_occurred"
                    }

                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.template_error_failed_to_export_bookmarks,
                            error.shortSummary
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            .disposeOnDestroy(viewLifecycleOwner)
    }

    private fun openIssueReport() {
        openUrl(
            url = getKoin().getProperty("issueReportingUrl")!!
        )
    }

    private fun openUrl(url: String) {
        val uri = Uri.parse(url)
        CustomTabsHelper.safelyLaunchUrl(
            requireContext(),
            CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setUrlBarHidingEnabled(true)
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .build(),
            uri
        )
    }

    private fun <T : Preference> PreferenceScreen.requirePreference(@StringRes keyId: Int): T {
        val key = getString(keyId)
        return findPreference<T>(key).checkNotNull {
            "Required preference '$key' not found"
        }
    }
}