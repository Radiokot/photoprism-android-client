package ua.com.radiokot.photoprism.features.ext.key.activation.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityKeyActivationBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.ext.key.activation.view.model.KeyActivationViewModel
import ua.com.radiokot.photoprism.features.ext.key.renewal.view.KeyRenewalActivity
import ua.com.radiokot.photoprism.util.SoftInputVisibility

class KeyActivationActivity : BaseActivity() {
    private val log = kLogger("KeyActivationActivity")

    private lateinit var view: ActivityKeyActivationBinding
    val viewModel: KeyActivationViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        view = ActivityKeyActivationBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        subscribeToState()
        subscribeToEvents()

        intent.data?.also(::onIntentData)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.also(::onIntentData)
    }

    private fun subscribeToState() = viewModel.state.subscribe(this) { state ->
        log.debug {
            "subscribeToState(): received_new_state:" +
                    "\nstate=$state"
        }

        when (state) {
            KeyActivationViewModel.State.Input -> {
                showInput()
            }

            is KeyActivationViewModel.State.Success -> {
                SoftInputVisibility.hide(window)
                // Ensure the keyboard will not re-appear.
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

                showSuccess()
            }
        }

        log.debug {
            "subscribeToState(): handled_new_state:" +
                    "\nstate=$state"
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            KeyActivationViewModel.Event.Finish ->
                finish()

            is KeyActivationViewModel.Event.ShowFloatingError ->
                showFloatingError(event.error)

            is KeyActivationViewModel.Event.LaunchHelpEmailIntent ->
                startActivity(Intent.createChooser(event.intent, getString(R.string.learn_more)))

            is KeyActivationViewModel.Event.OpenRenewal ->
                startActivity(
                    Intent(this, KeyRenewalActivity::class.java)
                        .putExtras(
                            KeyRenewalActivity.getBundle(
                                key = event.key,
                            )
                        )
                )
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }

    private fun showInput() {
        if (supportFragmentManager.findFragmentByTag(INPUT_FRAGMENT_TAG) != null) {
            log.debug {
                "showInput(): already_shown"
            }

            return
        }

        supportFragmentManager.commit {
            replace(R.id.fragment_container, KeyActivationInputFragment(), INPUT_FRAGMENT_TAG)
            disallowAddToBackStack()

            log.debug {
                "showInput(): showing"
            }
        }
    }

    private fun showSuccess() {
        if (supportFragmentManager.findFragmentByTag(SUCCESS_FRAGMENT_TAG) != null) {
            log.debug {
                "showSuccess(): already_shown"
            }

            return
        }

        supportFragmentManager.commit {
            replace(R.id.fragment_container, KeyActivationSuccessFragment(), SUCCESS_FRAGMENT_TAG)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            disallowAddToBackStack()

            log.debug {
                "showSuccess(): showing"
            }
        }
    }

    private fun showFloatingError(error: KeyActivationViewModel.Error) {
        Snackbar.make(view.fragmentContainer, error.localizedMessage, Snackbar.LENGTH_SHORT)
            .apply {
                if (error is KeyActivationViewModel.Error.KeyError.DeviceMismatch) {
                    setAction(R.string.key_activation_renew) {
                        viewModel.onKeyActivationErrorRenewClicked()
                    }
                }
            }
            .show()
    }

    private fun onIntentData(data: Uri) {
        val expectedScheme = getString(R.string.uri_scheme)
        if (data.scheme != getString(R.string.uri_scheme)) {
            log.error {
                "onIntentData(): scheme_mismatch:" +
                        "\nexpected=$expectedScheme," +
                        "\nactual=${data.scheme}," +
                        "\nuri=$data"
            }

            return
        }

        val keyParam = data.getQueryParameter("key")
        if (keyParam == null) {
            log.error {
                "onIntentData(): missing_key:" +
                        "\nuri=$data"
            }

            return
        }

        viewModel.onKeyPassedWithIntent(keyParam)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.key_activation, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.get_help) {
            viewModel.onGetHelpClicked()
        }

        return super.onOptionsItemSelected(item)
    }

    private val KeyActivationViewModel.Error.localizedMessage: String
        get() = when (this) {
            KeyActivationViewModel.Error.KeyError.DeviceMismatch ->
                getString(R.string.key_activation_error_device_mismatch)

            KeyActivationViewModel.Error.KeyError.EmailMismatch ->
                getString(R.string.key_activation_error_email_mismatch)

            KeyActivationViewModel.Error.KeyError.Expired ->
                getString(R.string.key_activation_error_expired)

            KeyActivationViewModel.Error.KeyError.Invalid ->
                getString(R.string.key_activation_error_invalid)

            KeyActivationViewModel.Error.KeyError.NoNewExtensions ->
                getString(R.string.key_activation_error_no_new_extensions)

            is KeyActivationViewModel.Error.FailedProcessing ->
                getString(
                    R.string.template_key_activation_error_failed_processing,
                    shortSummary,
                )
        }

    private companion object {
        private const val INPUT_FRAGMENT_TAG = "input"
        private const val SUCCESS_FRAGMENT_TAG = "success"
    }
}
