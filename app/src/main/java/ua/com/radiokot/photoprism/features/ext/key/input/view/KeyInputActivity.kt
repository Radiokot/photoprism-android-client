package ua.com.radiokot.photoprism.features.ext.key.input.view

import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityKeyInputBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.ext.key.input.view.model.KeyInputViewModel

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
                showSuccess()
            }
        }

        log.debug {
            "subscribeToState(): handled_new_state:" +
                    "\nstate=$state"
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

    private companion object {
        private const val ENTERING_FRAGMENT_TAG = "entering"
        private const val SUCCESS_FRAGMENT_TAG = "success"
    }
}
