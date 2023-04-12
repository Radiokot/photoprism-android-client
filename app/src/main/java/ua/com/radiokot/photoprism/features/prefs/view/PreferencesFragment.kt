package ua.com.radiokot.photoprism.features.prefs.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createFragmentScope
import org.koin.core.qualifier._q
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.features.envconnection.logic.DisconnectFromEnvUseCase
import ua.com.radiokot.photoprism.features.envconnection.view.EnvConnectionActivity
import ua.com.radiokot.photoprism.features.gallery.di.ImportSearchBookmarksUseCaseParams
import ua.com.radiokot.photoprism.features.gallery.logic.ExportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.ImportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.SearchBookmarksBackup
import ua.com.radiokot.photoprism.util.CustomTabsHelper

class PreferencesFragment : PreferenceFragmentCompat(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createFragmentScope().apply {
            linkTo(getScope(DI_SCOPE_SESSION))
        }
    }


    private val log = kLogger("PreferencesFragment")
    private val bookmarksBackup: SearchBookmarksBackup by inject()
    private val bookmarksBackupFileOpeningLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
            this::importBookmarksFromFile
        )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        initCustomTabs()
        initPreferences()
    }

    private fun initCustomTabs() {
        CustomTabsHelper.safelyConnectAndInitialize(requireContext())
    }

    private fun initPreferences() = preferenceScreen.apply {
        with(requirePreference(R.string.pk_library_api_url)) {
            summary = get<EnvSession>().envConnectionParams.apiUrl
        }

        with(requirePreference(R.string.pk_library_disconnect)) {
            setOnPreferenceClickListener {
                disconnect()
                true
            }
        }

        with(requirePreference(R.string.pk_import_bookmarks)) {
            setOnPreferenceClickListener {
                importBookmarks()
                true
            }
        }

        with(requirePreference(R.string.pk_export_bookmarks)) {
            setOnPreferenceClickListener {
                exportBookmarks()
                true
            }
        }

        with(requirePreference(R.string.pk_app_version)) {
            summary = BuildConfig.VERSION_NAME
        }

        with(requirePreference(R.string.pk_report_issue)) {
            setOnPreferenceClickListener {
                openIssueReport()
                true
            }
        }
    }

    private fun disconnect() {
        log.debug { "disconnect(): begin_disconnect" }

        get<DisconnectFromEnvUseCase>()
            .perform()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = ::goToEnvConnection,
                onError = { error ->
                    log.error(error) {
                        "disconnect(): error_occurred"
                    }

                    showError(
                        getString(
                            R.string.template_error_failed_to_disconnect,
                            error.shortSummary
                        )
                    )
                }
            )
            .disposeOnDestroy(viewLifecycleOwner)
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

                    showError(
                        text = getString(
                            R.string.template_error_failed_to_export_bookmarks,
                            error.shortSummary
                        ),
                    )
                }
            )
            .disposeOnDestroy(viewLifecycleOwner)
    }

    private fun importBookmarks() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_bookmarks)
            .setMessage(R.string.your_bookmarks_will_be_replaced)
            .setPositiveButton(R.string.ccontinue) { _, _ ->
                bookmarksBackupFileOpeningLauncher
                    .launch(arrayOf(bookmarksBackup.fileMimeType))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun importBookmarksFromFile(fileUri: Uri?) {
        if (fileUri == null) {
            log.error { "importBookmarksFromFile(): no_file_uri_provided" }
            return
        }

        log.debug {
            "importBookmarksFromFile(): begin_import:" +
                    "\nfileUri=$fileUri"
        }

        get<ImportSearchBookmarksUseCase>(_q<ImportSearchBookmarksUseCaseParams>()) {
            ImportSearchBookmarksUseCaseParams(
                fileUri = fileUri,
            )
        }
            .perform()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { importedBookmarks ->
                    val count = importedBookmarks.size

                    log.debug {
                        "importBookmarksFromFile(): successfully_imported:" +
                                "\ncount=$count"
                    }

                    Snackbar.make(
                        listView,
                        getString(
                            R.string.template_successfully_imported_bookmarks_plural,
                            count,
                            resources.getQuantityString(R.plurals.search_bookmarks, count)
                        ),
                        Snackbar.LENGTH_LONG,
                    ).show()
                },
                onError = { error ->
                    log.error(error) {
                        "importBookmarksFromFile(): error_occurred"
                    }

                    showError(
                        text = getString(
                            R.string.template_error_failed_to_import_bookmarks,
                            error.shortSummary
                        ),
                    )
                },
            )
            .disposeOnDestroy(viewLifecycleOwner)
    }

    private fun showError(text: String) {
        Snackbar.make(
            listView,
            text,
            Snackbar.LENGTH_LONG,
        ).show()
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

    private fun goToEnvConnection() {
        log.debug {
            "goToEnvConnection(): going_to_env_connection"
        }

        startActivity(Intent(requireContext(), EnvConnectionActivity::class.java))
        requireActivity().finishAffinity()
    }

    private fun PreferenceScreen.requirePreference(@StringRes keyId: Int): Preference {
        val key = getString(keyId)
        return findPreference<Preference>(key).checkNotNull {
            "Required preference '$key' not found"
        }
    }
}