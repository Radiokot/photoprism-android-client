package ua.com.radiokot.photoprism.features.ext.key.logic

import android.content.Intent
import android.net.Uri

/**
 * Creates an intent launching the email client
 * with a pre-composed message asking for help with the extensions.
 */
class CreateExtensionsHelpEmailUseCase(
    private val helpEmailAddress: String,
    private val hardwareIdentifier: HardwareIdentifier,
) {

    operator fun invoke(): Intent =
        Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(helpEmailAddress))
            .putExtra(Intent.EXTRA_SUBJECT, "Help with gallery extensions")
            .putExtra(
                Intent.EXTRA_TEXT,
                "My identifier (do not remove this): ${hardwareIdentifier.getHardwareIdentifier()}" +
                        "\n\n" +
                        "My question: "
            )
            .apply {
                // This allows selecting only email clients
                // while keeping the text from extras which gets lost with SENDTO.
                selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            }
}
