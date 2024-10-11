package ua.com.radiokot.photoprism.features.prefs.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.ListPreference
import androidx.preference.MaterialPreferenceDialogDisplay
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
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
import ua.com.radiokot.photoprism.extension.capitalized
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.shortSummary
import ua.com.radiokot.photoprism.featureflags.extension.hasExtensionStore
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.envconnection.logic.DisconnectFromEnvUseCase
import ua.com.radiokot.photoprism.features.ext.key.activation.view.KeyActivationActivity
import ua.com.radiokot.photoprism.features.ext.prefs.view.GalleryExtensionPreferencesFragment
import ua.com.radiokot.photoprism.features.ext.store.view.GalleryExtensionStoreActivity
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferences
import ua.com.radiokot.photoprism.features.gallery.di.ImportSearchBookmarksUseCaseParams
import ua.com.radiokot.photoprism.features.gallery.search.data.storage.SearchPreferences
import ua.com.radiokot.photoprism.features.gallery.search.logic.ExportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.search.logic.ImportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchBookmarksBackup
import ua.com.radiokot.photoprism.features.prefs.extension.bindToSubject
import ua.com.radiokot.photoprism.features.prefs.extension.requirePreference
import ua.com.radiokot.photoprism.features.viewer.slideshow.data.model.SlideshowSpeed
import ua.com.radiokot.photoprism.features.viewer.slideshow.data.storage.SlideshowPreferences
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.util.SafeCustomTabs
import java.util.Locale

class PreferencesFragment :
    PreferenceFragmentCompat(),
    AndroidScopeComponent,
    OnPreferenceDisplayDialogCallback by MaterialPreferenceDialogDisplay() {

    override val scope: Scope by lazy {
        getKoin().getScope(DI_SCOPE_SESSION)
            .apply { linkTo(createFragmentScope()) }
    }

    private val log = kLogger("PreferencesFragment")
    private val galleryPreferences: GalleryPreferences by inject()
    private val slideshowPreferences: SlideshowPreferences by inject()
    private val bookmarksBackup: SearchBookmarksBackup by inject()
    private val bookmarksBackupFileOpeningLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
            this::importBookmarksFromFile
        )
    private val session: EnvSession by inject()
    private val searchPreferences: SearchPreferences by inject()
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
    private val featureFlags: FeatureFlags by inject()
    private val locale: Locale by inject()

    private val issueReportingUrl: String = getKoin()
        .getProperty<String>("issueReportingUrl")
        .checkNotNull { "Missing issue reporting URL" }
    private val guidesSummaryUrl: String = getKoin()
        .getProperty<String>("guidesSummaryUrl")
        .checkNotNull { "Missing guides summary URL" }
    private val sourceCodeUrl: String = getKoin()
        .getProperty<String>("sourceCodeUrl")
        .checkNotNull { "Missing source code URL" }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun setPreferenceScreen(preferenceScreen: PreferenceScreen) {
        initPreferenceVisibility(preferenceScreen)
        super.setPreferenceScreen(preferenceScreen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log.debug {
            "onViewCreated(): start_init:" +
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
        SafeCustomTabs.safelyConnectAndInitialize(requireContext())
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

    private fun initPreferenceVisibility(
        preferenceScreen: PreferenceScreen
    ) = with(preferenceScreen) {
        with(requirePreference(R.string.pk_language)) {
            isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && getAppLanguagePreferencesIntent().resolveActivity(requireContext().packageManager) != null
        }
    }

    private fun initPreferences() = with(preferenceScreen) {
        with(requirePreference(R.string.pk_library)) {
            summary = session.envConnectionParams.rootUrl.toString()
            setOnPreferenceClickListener {
                disconnect()
                true
            }
        }

        with(requirePreference(R.string.pk_gallery_item_scale)) {
            this as ListPreference
            entries = resources.getStringArray(R.array.gallery_item_scale_array)
            entryValues = GalleryItemScale.values().map(GalleryItemScale::name).toTypedArray()
            value = galleryPreferences.itemScale.value!!.name
            setOnPreferenceChangeListener { _, newValue ->
                galleryPreferences.itemScale.onNext(GalleryItemScale.valueOf(newValue as String))
                true
            }
        }

        with(requirePreference(R.string.pk_slideshow_speed)) {
            this as ListPreference
            entries = resources.getStringArray(R.array.slideshow_speed_array)
            entryValues = SlideshowSpeed.values().map(SlideshowSpeed::name).toTypedArray()
            value = slideshowPreferences.speed.name
            setOnPreferenceChangeListener { _, newValue ->
                slideshowPreferences.speed = SlideshowSpeed.valueOf(newValue as String)
                true
            }
        }

        with(requirePreference(R.string.pk_live_photos_as_images)){
            this as SwitchPreferenceCompat
            bindToSubject(galleryPreferences.livePhotosAsImages, viewLifecycleOwner)
        }

        with(requirePreference(R.string.pk_show_people)) {
            this as SwitchPreferenceCompat
            bindToSubject(searchPreferences.showPeople, viewLifecycleOwner)
        }

        with(requirePreference(R.string.pk_show_albums)) {
            this as SwitchPreferenceCompat
            bindToSubject(searchPreferences.showAlbums, viewLifecycleOwner)
        }

        with(requirePreference(R.string.pk_show_album_folders)) {
            this as SwitchPreferenceCompat
            bindToSubject(searchPreferences.showAlbumFolders, viewLifecycleOwner)
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

        with(requirePreference(R.string.pk_language)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                summary = locale.displayLanguage.capitalized(locale)
                setOnPreferenceClickListener {
                    openAppLanguagePreferences()
                    true
                }
            }
        }

        with(requirePreference(R.string.pk_extensions)) {
            setOnPreferenceClickListener {
                val hasAnyExtensionsWithPreferences =
                    GalleryExtensionPreferencesFragment.EXTENSIONS_WITH_PREFERENCES
                        .any { it.feature in featureFlags }
                if (!hasAnyExtensionsWithPreferences) {
                    startActivity(
                        Intent(
                            requireContext(),
                            if (featureFlags.hasExtensionStore)
                                GalleryExtensionStoreActivity::class.java
                            else
                                KeyActivationActivity::class.java
                        )
                    )
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun disconnect() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.disconnect_from_library_confirmation)
            .setPositiveButton(R.string.disconnect_from_library) { _, _ ->
                log.debug { "disconnect(): begin_disconnect" }

                get<DisconnectFromEnvUseCase>()
                    .invoke()
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
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportBookmarks() {
        log.debug { "exportBookmarks(): begin_export" }

        get<ExportSearchBookmarksUseCase>()
            .invoke()
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
            .invoke()
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
        SafeCustomTabs.launchWithFallback(
            context = requireContext(),
            intent = CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setUrlBarHidingEnabled(true)
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .build(),
            url = issueReportingUrl,
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
                        url = guidesSummaryUrl,
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
        SafeCustomTabs.launchWithFallback(
            context = requireContext(),
            intent = CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setUrlBarHidingEnabled(true)
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                .build(),
            url = sourceCodeUrl,
            titleRes = R.string.app_name
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun openAppLanguagePreferences() =
        startActivity(getAppLanguagePreferencesIntent())

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getAppLanguagePreferencesIntent() =
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
            .setData(Uri.fromParts("package", requireContext().packageName, null))

    private companion object {
        private const val BOOKMARKS_EXPORT_OPTIONS_DIALOG_TAG = "beo"
        private const val BOOKMARKS_EXPORT_INTENT_KEY = "beo-intent"
    }
}
