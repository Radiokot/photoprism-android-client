package ua.com.radiokot.photoprism.features.envconnection.view

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.KeyChain
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.getKoin
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityEnvConnectionBinding
import ua.com.radiokot.photoprism.databinding.IncludeEnvConnectionFieldsBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.envconnection.view.model.EnvConnectionViewModel
import ua.com.radiokot.photoprism.features.gallery.view.GalleryActivity
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity
import ua.com.radiokot.photoprism.util.SoftInputVisibility

class EnvConnectionActivity : BaseActivity() {
    private val log = kLogger("EEnvConnectionActivity")

    private lateinit var view: ActivityEnvConnectionBinding
    private lateinit var fields: IncludeEnvConnectionFieldsBinding
    private val viewModel: EnvConnectionViewModel by viewModel()
    private val clientCertificateGuideUrl: String = getKoin()
        .getProperty<String>("clientCertificatesGuideUrl")
        .checkNotNull { "Missing client certificate guide URL" }
    private val connectionGuideUrl: String = getKoin()
        .getProperty<String>("connectionGuideUrl")
        .checkNotNull { "Missing connection guide URL" }
    private val webViewerForRedirectHandlingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onWebViewerRedirectHandlingResult,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityEnvConnectionBinding.inflate(layoutInflater)
        // Notice: there are layouts for multiple dimens.
        fields = IncludeEnvConnectionFieldsBinding.bind(view.root)
        setContentView(view.root)

        viewModel.initOnce(
            rootUrl = intent.getStringExtra(ROOT_URL_KEY),
        )

        initFields()
        initButtons()
        initTfaCodeDialog()

        subscribeToState()
        subscribeToEvents()
    }

    private fun initFields() = fields.apply {
        with(rootUrlTextInput) {
            editText!!.bindTextTwoWay(viewModel.rootUrl)

            viewModel.rootUrlError.observe(this@EnvConnectionActivity) { rootUrlError ->
                if (rootUrlError != null) {
                    isErrorEnabled = true
                    error = rootUrlError.localizedMessage
                } else {
                    isErrorEnabled = false
                    error = null
                }
            }

            setEndIconOnClickListener {
                viewModel.onRootUrlGuideButtonClicked()
            }
        }

        with(usernameTextInput) {
            editText!!.bindTextTwoWay(viewModel.username)
        }

        with(passwordTextInput) {
            editText!!.bindTextTwoWay(viewModel.password)

            editText!!.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    viewModel.onPasswordSubmitted()
                } else {
                    false
                }
            }

            viewModel.passwordError.observe(this@EnvConnectionActivity) { passwordError ->
                if (passwordError != null) {
                    isErrorEnabled = true
                    error = passwordError.localizedMessage
                } else {
                    isErrorEnabled = false
                    error = null
                }
            }
        }

        with(certificateTextInput) {
            isVisible = viewModel.isClientCertificateSelectionAvailable

            setOnClickListener {
                viewModel.onCertificateFieldClicked()
            }
            editText!!.setOnClickListener {
                viewModel.onCertificateFieldClicked()
            }

            viewModel.clientCertificateAlias.observe(this@EnvConnectionActivity) { alias ->
                editText?.setText(alias ?: "")
                isEndIconVisible = alias != null
            }

            isEndIconVisible = false
            setEndIconOnClickListener {
                viewModel.onCertificateClearButtonClicked()
            }
        }
    }

    private fun initButtons() {
        with(view.connectButton) {
            viewModel.isConnectButtonEnabled
                .observe(this@EnvConnectionActivity, this::setEnabled)

            setOnClickListener {
                viewModel.onConnectButtonClicked()
            }
        }

        view.titleTextView.setOnLongClickListener {
            viewModel.onTitleLongClicked()
            true
        }
    }

    private fun initTfaCodeDialog() {
        supportFragmentManager.setFragmentResultListener(
            TfaCodeDialogFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            viewModel.onTfaCodeEntered(
                enteredTfaCode = TfaCodeDialogFragment.getResult(bundle),
            )
        }
    }

    private fun subscribeToState() {
        viewModel.state.subscribeBy { state ->
            log.debug {
                "subscribeToState(): received_new_state:" +
                        "\nstate=$state"
            }

            view.progressIndicator.visibility = when (state) {
                EnvConnectionViewModel.State.Connecting ->
                    View.VISIBLE

                EnvConnectionViewModel.State.Idle ->
                    View.GONE
            }

            view.connectButton.visibility = when (state) {
                EnvConnectionViewModel.State.Connecting ->
                    View.GONE

                EnvConnectionViewModel.State.Idle ->
                    View.VISIBLE
            }

            when (state) {
                EnvConnectionViewModel.State.Connecting -> {
                    SoftInputVisibility.hide(window)
                    // Ensure the keyboard will not re-appear.
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
                }

                EnvConnectionViewModel.State.Idle -> {
                }
            }

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }.autoDispose(this)
    }

    private fun subscribeToEvents() {
        viewModel.events.subscribe { event ->
            log.debug {
                "subscribeToEvents(): received_new_event:" +
                        "\nevent=$event"
            }

            when (event) {
                EnvConnectionViewModel.Event.GoToGallery ->
                    goToGallery()

                EnvConnectionViewModel.Event.ChooseClientCertificateAlias ->
                    chooseClientCertificateAlias()

                EnvConnectionViewModel.Event.ShowMissingClientCertificatesNotice ->
                    showMissingClientCertificatesNotice()

                is EnvConnectionViewModel.Event.OpenConnectionGuide ->
                    openConnectionGuide()

                is EnvConnectionViewModel.Event.OpenClientCertificateGuide ->
                    openClientCertificateGuide()

                is EnvConnectionViewModel.Event.OpenWebViewerForRedirectHandling ->
                    openWebViewerForRedirectHandling(
                        url = event.url,
                    )

                is EnvConnectionViewModel.Event.RequestTfaCodeInput ->
                    showTfaCodeDialog()

                is EnvConnectionViewModel.Event.ShowFloatingError ->
                    showFloatingError(
                        error = event.error,
                    )
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.autoDispose(this)
    }

    private fun chooseClientCertificateAlias() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        log.debug {
            "chooseClientCertificateAlias(): opening_chooser"
        }

        val now = System.currentTimeMillis()
        KeyChain.choosePrivateKeyAlias(this, { alias ->
            if (alias != null) {
                viewModel.onCertificateAliasChosen(alias)
            } else {
                val elapsed = System.currentTimeMillis() - now

                // 🤡 An elegant way to determine whether the request is cancelled,
                // or there are not certificates.
                // Thanks, Android, for a meaningful result callback.
                if (elapsed < 1000) {
                    viewModel.onNoCertificatesAvailable()
                } else {
                    log.debug {
                        "chooseClientCertificateAlias(): no_alias_chosen"
                    }
                }
            }
        }, null, null, null, null)
    }

    private fun showMissingClientCertificatesNotice() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.how_to_use_client_certificate)
            .setMessage(R.string.p12_certificate_guide)
            .setPositiveButton(R.string.ok) { _, _ -> }
            .setNeutralButton(R.string.learn_more) { _, _ ->
                viewModel.onCertificateLearnMoreButtonClicked()
            }
            .show()
    }

    private fun openConnectionGuide() {
        startActivity(
            Intent(this, WebViewActivity::class.java).putExtras(
                WebViewActivity.getBundle(
                    url = connectionGuideUrl,
                    titleRes = R.string.connect_to_a_library,
                    pageStartedInjectionScripts = setOf(
                        WebViewInjectionScriptFactory.Script.GITHUB_WIKI_IMMERSIVE,
                    ),
                )
            )
        )
    }

    private fun openClientCertificateGuide() {
        startActivity(
            Intent(this, WebViewActivity::class.java).putExtras(
                WebViewActivity.getBundle(
                    url = clientCertificateGuideUrl,
                    titleRes = R.string.how_to_use_client_certificate,
                    pageStartedInjectionScripts = setOf(
                        WebViewInjectionScriptFactory.Script.GITHUB_WIKI_IMMERSIVE,
                    ),
                )
            )
        )
    }

    private fun openWebViewerForRedirectHandling(url: String) =
        webViewerForRedirectHandlingLauncher.launch(
            Intent(this, WebViewActivity::class.java)
                .putExtras(
                    WebViewActivity.getBundle(
                        url = url,
                        titleRes = R.string.connect_to_a_library,
                        finishOnRedirectEnd = true,
                    )
                )
        )

    private fun onWebViewerRedirectHandlingResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onWebViewerHandledRedirect()
        }
    }

    private fun showTfaCodeDialog() {
        if (supportFragmentManager.findFragmentByTag(TfaCodeDialogFragment.TAG) == null) {
            TfaCodeDialogFragment()
                .show(supportFragmentManager, TfaCodeDialogFragment.TAG)
        }
    }

    private fun showFloatingError(error: EnvConnectionViewModel.Error) {
        Snackbar.make(view.root, error.localizedMessage, Snackbar.LENGTH_SHORT)
            .show()
    }

    private val EnvConnectionViewModel.Error.localizedMessage: String
        get() = when (this) {
            EnvConnectionViewModel.Error.InvalidTfaCode ->
                getString(R.string.error_invalid_tfa_code)

            EnvConnectionViewModel.Error.PasswordError.Invalid ->
                getString(R.string.error_invalid_password)

            is EnvConnectionViewModel.Error.RootUrlError.Inaccessible ->
                getString(
                    R.string.template_error_inaccessible_library_url,
                    shortSummary,
                )

            EnvConnectionViewModel.Error.RootUrlError.InvalidFormat ->
                getString(R.string.error_invalid_library_url_format)

            EnvConnectionViewModel.Error.RootUrlError.RequiresCredentials ->
                getString(R.string.error_library_requires_credentials)
        }

    private fun goToGallery() {
        log.debug {
            "goToGallery(): going_to_gallery"
        }

        startActivity(Intent(this, GalleryActivity::class.java))
        finishAffinity()
    }

    companion object {
        private const val ROOT_URL_KEY = "root_url"

        fun getBundle(rootUrl: String?) = Bundle().apply {
            putString(ROOT_URL_KEY, rootUrl)
        }
    }
}
