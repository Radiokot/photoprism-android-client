package ua.com.radiokot.photoprism.features.ext.key.renewal.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityKeyRenewalBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.extension.subscribe
import ua.com.radiokot.photoprism.features.ext.key.activation.view.KeyActivationActivity
import ua.com.radiokot.photoprism.features.ext.key.renewal.view.model.KeyRenewalViewModel

class KeyRenewalActivity : BaseActivity() {

    private val log = kLogger("KeyRenewalActivity")
    private lateinit var view: ActivityKeyRenewalBinding
    private val viewModel: KeyRenewalViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityKeyRenewalBinding.inflate(layoutInflater)
        setContentView(view.root)

        viewModel.init(
            key = intent.getStringExtra(KEY_EXTRA)
                ?: error("Missing $KEY_EXTRA")
        )

        initToolbar()
        initKeyField()
        initContinueButton()

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
            maxLines = 3
            setHorizontallyScrolling(false)
        }

        viewModel.state.subscribe(this@KeyRenewalActivity) { state ->
            editText?.setText(
                when (state) {
                    is KeyRenewalViewModel.State.Done ->
                        state.newKey

                    is KeyRenewalViewModel.State.Preparing ->
                        state.key
                }
            )

            isEndIconVisible = state is KeyRenewalViewModel.State.Done

            hint = getString(
                when (state) {
                    is KeyRenewalViewModel.State.Done ->
                        R.string.key_renewal_new_key_title

                    is KeyRenewalViewModel.State.Preparing ->
                        R.string.key_renewal_current_key_title
                }
            )
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
    }

    private fun subscribeToEvents() = viewModel.events.subscribe(this) { event ->
        log.debug {
            "subscribeToEvents(): received_new_event:" +
                    "\nevent=$event"
        }

        when (event) {
            is KeyRenewalViewModel.Event.GoToActivation -> {
                startActivity(
                    Intent(this, KeyActivationActivity::class.java)
                        .setData(
                            Uri.Builder()
                                .scheme(getString(R.string.uri_scheme))
                                .appendQueryParameter("key", event.key)
                                .build()
                        )
                )
                finish()
            }
        }
    }

    companion object {
        private const val KEY_EXTRA = "key"

        fun getBundle(key: String) = Bundle().apply {
            putString(KEY_EXTRA, key)
        }
    }
}
