package ua.com.radiokot.photoprism.features.envconnection.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.KeyChain
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityEnvConnectionBinding
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.disposeOnDestroy
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.envconnection.view.model.EnvConnectionViewModel
import ua.com.radiokot.photoprism.features.gallery.view.GalleryActivity
import ua.com.radiokot.photoprism.util.SoftInputUtil

class EnvConnectionActivity : BaseActivity() {
    private val log = kLogger("EEnvConnectionActivity")

    private lateinit var view: ActivityEnvConnectionBinding
    private val viewModel: EnvConnectionViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityEnvConnectionBinding.inflate(layoutInflater)
        setContentView(view.root)

        initFields()
        initButtons()

        subscribeToState()
        subscribeToEvents()
    }

    private fun initFields() {
        with(view.rootUrlTextInput) {
            editText!!.bindTextTwoWay(viewModel.rootUrl)

            viewModel.rootUrlError.observe(this@EnvConnectionActivity) { rootUrlError ->
                when (rootUrlError) {
                    is EnvConnectionViewModel.RootUrlError.Inaccessible -> {
                        isErrorEnabled = true
                        error = getString(
                            R.string.template_error_inaccessible_library_url,
                            rootUrlError.shortSummary,
                        )
                    }
                    EnvConnectionViewModel.RootUrlError.InvalidFormat -> {
                        isErrorEnabled = true
                        error = getString(R.string.error_invalid_library_url_format)
                    }
                    EnvConnectionViewModel.RootUrlError.RequiresCredentials -> {
                        isErrorEnabled = true
                        error = getString(R.string.error_library_requires_credentials)
                    }
                    null -> {
                        isErrorEnabled = false
                        error = null
                    }
                }
            }
        }

        with(view.usernameTextInput) {
            editText!!.bindTextTwoWay(viewModel.username)
        }

        with(view.passwordTextInput) {
            editText!!.bindTextTwoWay(viewModel.password)

            editText!!.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    viewModel.onPasswordSubmitted()
                } else {
                    false
                }
            }

            viewModel.passwordError.observe(this@EnvConnectionActivity) { passwordError ->
                when (passwordError) {
                    EnvConnectionViewModel.PasswordError.Invalid -> {
                        isErrorEnabled = true
                        error = getString(R.string.error_invalid_password)
                    }
                    null -> {
                        isErrorEnabled = false
                        error = null
                    }
                }
            }
        }

        with(view.certificateTextInput) {
            isVisible = viewModel.isClientCertificateSelectionAvailable

            setOnClickListener {
                viewModel.onCertificateFieldClicked()
            }
            editText!!.setOnClickListener { viewModel.onCertificateFieldClicked() }

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
    }

    private fun subscribeToState() {
        viewModel.state.observe(this) { state ->
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
                    SoftInputUtil.hideSoftInput(window)
                }
                EnvConnectionViewModel.State.Idle -> {
                }
            }

            log.debug {
                "subscribeToState(): handled_new_state:" +
                        "\nstate=$state"
            }
        }
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
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.disposeOnDestroy(this)
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
            .setNeutralButton(R.string.learn_more) { _, _ -> }
            .show()
    }

    private fun goToGallery() {
        log.debug {
            "goToGallery(): going_to_gallery"
        }

        startActivity(Intent(this, GalleryActivity::class.java))
        finishAffinity()
    }
}