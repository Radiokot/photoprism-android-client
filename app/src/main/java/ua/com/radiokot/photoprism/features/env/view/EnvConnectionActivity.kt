package ua.com.radiokot.photoprism.features.env.view

import android.os.Bundle
import android.view.View
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityEnvConnectionBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.env.view.model.EnvConnectionViewModel

class EnvConnectionActivity : BaseActivity() {
    private val log = kLogger("EEnvConnectionActivity")

    private lateinit var view: ActivityEnvConnectionBinding
    private val viewModel: EnvConnectionViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityEnvConnectionBinding.inflate(layoutInflater)
        setContentView(view.root)

        viewModel.init()

        initFields()
        initButtons()
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

        viewModel.isPublic.observe(this@EnvConnectionActivity) { isPublic ->
            view.authButtonGroup.check(
                if (isPublic)
                    R.id.public_button
                else
                    R.id.private_button
            )
        }

        view.publicButton.setOnClickListener {
            viewModel.isPublic.value = true
        }

        view.privateButton.setOnClickListener {
            viewModel.isPublic.value = false
        }
    }
}