package ua.com.radiokot.photoprism.features.welcome.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.text.HtmlCompat
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityWelcomeBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.features.welcome.data.storage.WelcomeScreenPreferences
import ua.com.radiokot.photoprism.util.InternalLinkMovementMethod

class WelcomeActivity : BaseActivity() {
    private lateinit var view: ActivityWelcomeBinding

    private val welcomeScreenPreferences: WelcomeScreenPreferences by inject()
    private val helpEmailAddress: String = getKoin()
        .getProperty<String>("helpEmailAddress")
        .checkNotNull { "Missing help email address" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notice: there are layouts for multiple dimens.
        view = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(view.root)

        initText()
        initButtons()
    }

    private fun initText() {
        view.mainTextView.text = HtmlCompat.fromHtml(
            getString(
                R.string.template_welcome_text,
                helpEmailAddress,
            ),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

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
            welcomeScreenPreferences.isWelcomeNoticeAccepted = true
            goToEnvConnection()
        }
    }

    private fun safeView(url: String) = tryOrNull {
        // Do not use internal webviewer here, as it may of course have issues too.
        startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
    }
}
