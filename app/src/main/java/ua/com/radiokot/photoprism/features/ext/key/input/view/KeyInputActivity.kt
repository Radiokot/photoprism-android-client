package ua.com.radiokot.photoprism.features.ext.key.input.view

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityKeyInputBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.ext.key.input.view.model.KeyInputViewModel
import ua.com.radiokot.photoprism.util.SoftInputVisibility

class KeyInputActivity : BaseActivity() {
    private val log = kLogger("KeyInputActivity")

    private lateinit var view: ActivityKeyInputBinding
    val viewModel: KeyInputViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        view = ActivityKeyInputBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        subscribeToState()
        subscribeToEvents()
    }

    private fun subscribeToState() = viewModel.state.subscribeBy { state ->
        log.debug {
            "subscribeToState(): received_new_state:" +
                    "\nstate=$state"
        }

        when (state) {
            KeyInputViewModel.State.Entering -> {
                showEntering()
            }

            is KeyInputViewModel.State.SuccessfullyEntered -> {
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
    }.autoDispose(this)

    private fun subscribeToEvents() = viewModel.events.subscribeBy { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            KeyInputViewModel.Event.Finish ->
                finish()

            is KeyInputViewModel.Event.ShowFloatingFailedProcessingMessage ->
                showFloatingMessage(
                    getString(
                        R.string.template_key_input_error_failed_processing,
                        event.shortSummary
                    )
                )

            KeyInputViewModel.Event.ShowFloatingNoNewExtensionsMessage ->
                showFloatingMessage(getString(R.string.key_input_error_no_new_extensions))
        }

        log.debug {
            "subscribeToEvents(): handled_new_event:" +
                    "\nevent=$event"
        }
    }.autoDispose(this)

    private fun showEntering() {
        if (supportFragmentManager.findFragmentByTag(ENTERING_FRAGMENT_TAG) != null) {
            log.debug {
                "showEntering(): already_shown"
            }

            return
        }

        supportFragmentManager.commit {
            replace(R.id.fragment_container, KeyInputEnteringFragment(), ENTERING_FRAGMENT_TAG)
            disallowAddToBackStack()

            log.debug {
                "showEntering(): showing"
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
            replace(R.id.fragment_container, KeyInputSuccessFragment(), SUCCESS_FRAGMENT_TAG)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            disallowAddToBackStack()

            log.debug {
                "showSuccess(): showing"
            }
        }
    }

    private fun showFloatingMessage(message: String) {
        Snackbar.make(view.fragmentContainer, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    private companion object {
        private const val ENTERING_FRAGMENT_TAG = "entering"
        private const val SUCCESS_FRAGMENT_TAG = "success"
    }
}
