package ua.com.radiokot.photoprism.features.prefs.view

import android.content.Context
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
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import okio.buffer
import okio.sink
import okio.source
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createFragmentScope
import org.koin.core.qualifier._q
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.extension.withMaskedCredentials
import ua.com.radiokot.photoprism.features.envconnection.logic.DisconnectFromEnvUseCase
import ua.com.radiokot.photoprism.features.gallery.di.ImportSearchBookmarksUseCaseParams
import ua.com.radiokot.photoprism.features.gallery.logic.ExportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.ImportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.SearchBookmarksBackup
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.util.CustomTabsHelper

class PreferencesFragment : PreferenceFragmentCompat(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        getKoin().getScope(DI_SCOPE_SESSION)
            .apply { linkTo(createFragmentScope()) }
    }

    private val log = kLogger("PreferencesFragment")
    private val bookmarksBackup: SearchBookmarksBackup by inject()
    private val bookmarksBackupFileOpeningLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
            this::importBookmarksFromFile
        )
    private val session: EnvSession by inject()
    private var bookmarksExportResultIntent: Intent? = null
    private val bookmarksExportSaveDialogLauncher = registerForActivityResult(
        object : ActivityResultContracts.CreateDocument("*/*") {
            override fun createIntent(context: Context, input: String): Intent {
                return super.createIntent(context, input).apply {
                    // Can't obtain the type during the initialization,
                    // have to set it here.
                    type = bookmarksBackup.fileMimeType
                }
            }
        },
        this::writeBookmarksExportToFile
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        log.debug {
            "onCreatePreferences(): start_init:" +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        @Suppress("DEPRECATION")
        bookmarksExportResultIntent = savedInstanceState?.getParcelable(BOOKMARKS_EXPORT_INTENT_KEY)

        initCustomTabs()
        initBookmarksExportOptionsDialog()
        initPreferences()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // VIEWMODEL IS NOT ALWAYS A MUST. VIEWMODEL IS NOT ALWAYS A MUST. VIEWMODEL IS NOT ALWAYS A MUST.
        // VIEWMODEL IS NOT ALWAYS A MUST. VIEWMODEL IS NOT ALWAYS A MUST. VIEWMODEL IS NOT ALWAYS A MUST.
        // 👁_👁
        outState.putParcelable(BOOKMARKS_EXPORT_INTENT_KEY, bookmarksExportResultIntent)
    }

    private fun initCustomTabs() {
        CustomTabsHelper.safelyConnectAndInitialize(requireContext())
    }

    private fun initBookmarksExportOptionsDialog() {
        childFragmentManager.setFragmentResultListener(
            SearchBookmarksExportOptionsDialogFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            when (val optionId = SearchBookmarksExportOptionsDialogFragment.getResult(bundle)) {
                R.id.share_button ->
                    shareBookmarksExport()

                R.id.save_button ->
                    openBookmarksExportSaveDialog()

                else ->
                    log.debug {
                        "initBookmarksExportOptionsDialog(): got_unknown_option_id:" +
                                "\noptionId=$optionId"
                    }
            }
        }
    }

    private fun initPreferences() = preferenceScreen.apply {
        with(requirePreference(R.string.pk_library_root_url)) {
            summary = session.envConnectionParams.rootUrl
                .withMaskedCredentials()
                .toString()
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
            summary = getString(
                R.string.template_preferences_version,
                getString(R.string.app_name),
                BuildConfig.VERSION_NAME,
                getString(R.string.build_year),
            )

            setOnPreferenceClickListener {
                openSourceCode()
                true
            }
        }

        with(requirePreference(R.string.pk_report_issue)) {
            setOnPreferenceClickListener {
                openIssueReport()
                true
            }
        }

        with(requirePreference(R.string.pk_os_licenses)) {
            setOnPreferenceClickListener {
                openOpenSourceLicenses()
                true
            }
        }

        with(requirePreference(R.string.pk_guides)) {
            setOnPreferenceClickListener {
                openGuidesSummary()
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
            .autoDispose(viewLifecycleOwner)
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
                        "exportBookmarks(): asking_for_the_option:" +
                                "\nintent=$intent"
                    }

                    bookmarksExportResultIntent = intent

                    openBookmarksExportOptionsDialog()
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
            .autoDispose(viewLifecycleOwner)
    }

    private fun openBookmarksExportOptionsDialog() {
        // Activity fragment manager is used to keep the dialog opened
        // on activity re-creation.
        val fragment =
            (childFragmentManager.findFragmentByTag(BOOKMARKS_EXPORT_OPTIONS_DIALOG_TAG) as? SearchBookmarksExportOptionsDialogFragment)
                ?: SearchBookmarksExportOptionsDialogFragment()

        if (!fragment.isAdded || !fragment.showsDialog) {
            fragment.showNow(childFragmentManager, BOOKMARKS_EXPORT_OPTIONS_DIALOG_TAG)
        }
    }

    private fun shareBookmarksExport() {
        log.debug {
            "shareBookmarksExport(): starting_sharing_chooser"
        }

        startActivity(
            Intent.createChooser(
                bookmarksExportResultIntent,
                getString(R.string.share_the_file)
            )
        )
    }

    private fun openBookmarksExportSaveDialog() {
        log.debug {
            "openBookmarksExportSaveDialog(): starting_create_document_intent"
        }

        bookmarksExportSaveDialogLauncher.launch(
            bookmarksExportResultIntent!!.getStringExtra(Intent.EXTRA_TITLE).checkNotNull {
                "The bookmarks export result intent is expected to have the title extra"
            }
        )
    }

    private fun writeBookmarksExportToFile(outputFileUri: Uri?) {
        if (outputFileUri == null) {
            // The dialog has been cancelled.
            return
        }

        val contentResolver = requireContext().contentResolver
        val exportFileUri = bookmarksExportResultIntent!!.data.checkNotNull {
            "The bookmarks export result intent is expected to have the data URI"
        }

        log.debug {
            "writeBookmarksExportToFile(): start_writing:" +
                    "\noutputFileUri=$outputFileUri," +
                    "\nexportFileUri=$exportFileUri"
        }

        Completable.defer {
            contentResolver.openOutputStream(outputFileUri)?.use { outputStream ->
                contentResolver.openInputStream(exportFileUri)?.use { inputStream ->
                    inputStream.source().buffer().readAll(outputStream.sink())
                } ?: log.error {
                    "writeBookmarksExportToFile(): failed_to_open_input_stream"
                }
            } ?: log.error {
                "writeBookmarksExportToFile(): failed_to_open_output_stream"
            }
            Completable.complete()
        }
            .subscribeOn(Schedulers.io())
            .subscribeBy {
                log.debug {
                    "writeBookmarksExportToFile(): successfully_written"
                }
            }
            .autoDispose(viewLifecycleOwner)
    }

    private fun importBookmarks() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_bookmarks)
            .setMessage(R.string.your_bookmarks_will_be_replaced)
            .setPositiveButton(R.string.continuee) { _, _ ->
                bookmarksBackupFileOpeningLauncher
                    .launch(arrayOf(bookmarksBackup.fileMimeType))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun importBookmarksFromFile(fileUri: Uri?) {
        if (fileUri == null) {
            // The dialog has been cancelled.
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
                            R.string.template_successfully_imported_search_bookmarks,
                            resources.getQuantityString(
                                R.plurals.imported_search_bookmarks,
                                count,
                                count
                            )
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
            .autoDispose(viewLifecycleOwner)
    }

    private fun showError(text: String) {
        Snackbar.make(
            listView,
            text,
            Snackbar.LENGTH_LONG,
        ).show()
    }

    private fun openIssueReport() {
        // Custom tabs are preferred here,
        // as the user may be already logged into GitHub
        // and so won't have to log in again.
        CustomTabsHelper.launchWithFallback(
            context = requireContext(),
            intent = CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setUrlBarHidingEnabled(true)
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .build(),
            url = getKoin().getProperty("issueReportingUrl")!!,
            titleRes = R.string.report_an_issue
        )
    }

    private fun goToEnvConnection() {
        (requireActivity() as BaseActivity).goToEnvConnectionIfNoSession()
    }

    private fun openOpenSourceLicenses() {
        startActivity(
            Intent(requireContext(), WebViewActivity::class.java)
                .putExtras(
                    WebViewActivity.getBundle(
                        url = "file:///android_asset/open_source_licenses.html",
                        titleRes = R.string.used_open_source_software,
                        pageFinishedInjectionScripts = setOf(
                            WebViewInjectionScriptFactory.Script.SIMPLE_HTML_IMMERSIVE,
                        )
                    )
                )
        )
    }

    private fun openGuidesSummary() {
        startActivity(
            Intent(requireContext(), WebViewActivity::class.java)
                .putExtras(
                    WebViewActivity.getBundle(
                        url = getKoin().getProperty("guidesSummaryUrl")!!,
                        titleRes = R.string.user_guides,
                        pageFinishedInjectionScripts = setOf(
                            WebViewInjectionScriptFactory.Script.GITHUB_WIKI_IMMERSIVE,
                        )
                    )
                )
        )
    }

    private fun openSourceCode() {
        // Custom tabs are preferred here,
        // as the user may be already logged into GitHub
        // and so won't have to log in again.
        CustomTabsHelper.launchWithFallback(
            context = requireContext(),
            intent = CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setUrlBarHidingEnabled(true)
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .build(),
            url = getKoin().getProperty("sourceCodeUrl")!!,
            titleRes = R.string.app_name
        )
    }

    private fun PreferenceScreen.requirePreference(@StringRes keyId: Int): Preference {
        val key = getString(keyId)
        return findPreference<Preference>(key).checkNotNull {
            "Required preference '$key' not found"
        }
    }

    private companion object {
        private const val BOOKMARKS_EXPORT_OPTIONS_DIALOG_TAG = "beo"
        private const val BOOKMARKS_EXPORT_INTENT_KEY = "beo-intent"
    }
}
