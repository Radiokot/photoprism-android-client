package ua.com.radiokot.photoprism.features.welcome.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.koin.android.ext.android.getKoin
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityWelcomeBinding
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.util.InternalLinkMovementMethod

class WelcomeActivity : BaseActivity() {
    private lateinit var view: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(view.root)

        initText()
        initButtons()
    }

    private fun initText() {
        view.mainTextView.movementMethod = InternalLinkMovementMethod { url ->
            when (url) {
                "#issue" -> {
                    safeView(getKoin().getProperty("issueReportingUrl")!!)
                    true
                }

                else ->
                    false
            }
        }
    }

    private fun initButtons() {
        view.continueButton.setOnClickListener {
            goToEnvConnection()
        }
    }

    private fun safeView(url: String) = tryOrNull {
        startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
        finish()
    }
}
