package ua.com.radiokot.photoprism.features.envconnection.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.KeyChain
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.RequiresApi
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
        viewModel.areCredentialsVisible.observe(this) { areCredentialsVisible ->
            val visibility =
                if (areCredentialsVisible)
                    View.VISIBLE
                else
                    View.GONE

            view.passwordTextInput.visibility = visibility
            view.usernameTextInput.visibility = visibility

            if (areCredentialsVisible == false && !view.rootUrlTextInput.isFocused) {
                view.rootUrlTextInput.requestFocus()
            }
        }

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
                    EnvConnectionViewModel.RootUrlError.IsNotPublic -> {
                        isErrorEnabled = true
                        error = getString(R.string.error_library_is_not_public)
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

        view.certificateTextInput.editText!!.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                chooseClientCertificateAlias()
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
            }

            log.debug {
                "subscribeToEvents(): handled_new_event:" +
                        "\nevent=$event"
            }
        }.disposeOnDestroy(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun chooseClientCertificateAlias() {
        log.debug {
            "chooseClientCertificateAlias(): opening_chooser"
        }

        KeyChain.choosePrivateKeyAlias(this, { alias ->
            if (alias != null) {
                log.debug {
                    "chooseClientCertificateAlias(): alias_chosen:" +
                            "\nalias=$alias"
                }
                view.certificateTextInput.post {
                    view.certificateTextInput.editText?.setText(alias)
                }
            } else {
                log.debug {
                    "chooseClientCertificateAlias(): no_alias_chosen"
                }
            }

        }, null, null, null, null)
    }

    private fun goToGallery() {
        log.debug {
            "goToGallery(): going_to_gallery"
        }

        startActivity(Intent(this, GalleryActivity::class.java))
        finishAffinity()
    }
}