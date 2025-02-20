package ua.com.radiokot.photoprism.features.ext.key.renewal.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityKeyRenewalBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.ext.key.activation.view.KeyActivationActivity
import ua.com.radiokot.photoprism.features.ext.key.renewal.view.model.KeyRenewalViewModel
import kotlin.math.absoluteValue

class KeyRenewalActivity : BaseActivity() {

    private val log = kLogger("KeyRenewalActivity")
    private lateinit var view: ActivityKeyRenewalBinding
    private val viewModel: KeyRenewalViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityKeyRenewalBinding.inflate(layoutInflater)
        setContentView(view.root)

        viewModel.initOnce(
            key = intent.getStringExtra(KEY_EXTRA)
                ?: error("Missing $KEY_EXTRA")
        )

        initToolbar()
        initKeyField()
        initContinueButton()
        initProgress()

        subscribeToEvents()
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
    }

    private fun initKeyField() = with(view.keyTextInput) {
        with(editText!!) {
            inputType = InputType.TYPE_NULL
            setTextIsSelectable(true)
            // Only works if set programmatically on the EditText 🤷.
            maxLines = 10
            setHorizontallyScrolling(false)
        }

        var previousState: KeyRenewalViewModel.State? = null
        viewModel.state.subscribe(this@KeyRenewalActivity) { state ->

            // Crazy 360 on changing to Done!
            if (state is KeyRenewalViewModel.State.Done && previousState is KeyRenewalViewModel.State.Preparing) {
                var isModeSwitched = false

                animate()
                    .rotationX(360f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(
                        resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                    )
                    .setUpdateListener {
                        val absDistanceToMiddle = (0.5f - it.animatedValue as Float).absoluteValue
                        scaleX = 0.5f + absDistanceToMiddle
                        scaleY = 0.5f + absDistanceToMiddle

                        if (absDistanceToMiddle < 0.1f && !isModeSwitched) {
                            editText?.setText(state.newKey)
                            hint = getString(R.string.key_renewal_new_key_title)
                            isEndIconVisible = true

                            isModeSwitched = true
                        }
                    }
                    .start()
            } else {
                editText?.setText(
                    when (state) {
                        is KeyRenewalViewModel.State.Done ->
                            state.newKey

                        is KeyRenewalViewModel.State.Preparing ->
                            state.key
                    }
                )
                hint = when (state) {
                    is KeyRenewalViewModel.State.Done ->
                        getString(R.string.key_renewal_new_key_title)

                    is KeyRenewalViewModel.State.Preparing ->
                        getString(R.string.key_renewal_current_key_title)
                }
                isEndIconVisible = state is KeyRenewalViewModel.State.Done
            }

            previousState = state
        }

        setEndIconOnClickListener {
            viewModel.onCopyKeyClicked()
        }
    }

    private fun initContinueButton() = with(view.continueButton) {
        setThrottleOnClickListener {
            viewModel.onContinueClicked()
        }

        viewModel.state.subscribe(this@KeyRenewalActivity) { state ->
            text = getString(
                when (state) {
                    is KeyRenewalViewModel.State.Done ->
                        R.string.key_renewal_activate_new_key

                    is KeyRenewalViewModel.State.Preparing ->
                        R.string.continuee
                }
            )
        }

        viewModel.isLoading.observe(this@KeyRenewalActivity) { isLoading ->
            isVisible = !isLoading
        }
    }

    private fun initProgress() = with(view.progressIndicator) {
        viewModel.isLoading.observe(this@KeyRenewalActivity) { isLoading ->
            isVisible = isLoading
        }
    }

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            is KeyRenewalViewModel.Event.GoToActivation -> {
                goToActivation(
                    key = event.key,
                )
            }

            is KeyRenewalViewModel.Event.ShowFloatingError -> {
                showFloatingError(event.error)
            }
        }
    }

    private fun goToActivation(key: String) {
        startActivity(
            Intent(this, KeyActivationActivity::class.java)
                .setData(
                    Uri.Builder()
                        .scheme(getString(R.string.uri_scheme))
                        .appendQueryParameter("key", key)
                        .build()
                )
        )
        finish()
    }

    private fun showFloatingError(error: KeyRenewalViewModel.Error) {
        Snackbar.make(view.root, error.localizedMessage, Snackbar.LENGTH_SHORT)
            .apply {
                if (error is KeyRenewalViewModel.Error.NotAvailable) {
                    duration = Snackbar.LENGTH_LONG
                }
            }
            .show()
    }

    private val KeyRenewalViewModel.Error.localizedMessage: String
        get() = when (this) {
            KeyRenewalViewModel.Error.NotAvailable ->
                getString(R.string.key_renewal_error_not_available)

            KeyRenewalViewModel.Error.Failed ->
                getString(R.string.key_renewal_error_failed)
        }

    companion object {
        private const val KEY_EXTRA = "key"

        fun getBundle(key: String) = Bundle().apply {
            putString(KEY_EXTRA, key)
        }
    }
}
