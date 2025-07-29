package ua.com.radiokot.photoprism.features.envconnection.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import java.io.File

/**
 * A migration of the env connection params and credentials storage
 * from [EncryptedSharedPreferences] to just [SharedPreferences].
 *
 * It will be removed after a while, together with the security-crypto dependency.
 */
class EncryptedSharedPreferencesRemovalMigration(
    private val context: Context,
) {
    private val log = kLogger("EncryptedSharedPreferencesRemovalMigration")

    private val encryptedSharedPreferences: SharedPreferences? by lazy {
        tryOrNull {
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                MasterKey.Builder(context, ENCRYPTED_PREFS_NAME)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }

    val isApplicable: Boolean
        get() = getEncryptedPrefsFile().exists()
                && encryptedSharedPreferences != null

    fun apply(newPreferences: SharedPreferences) {

        log.debug {
            "apply(): applying"
        }

        newPreferences.edit {
            encryptedSharedPreferences!!
                .getString("session3", null)
                .also {
                    putString("session3", it)

                    log.debug {
                        "apply(): copied_session"
                    }
                }

            encryptedSharedPreferences!!
                .getString("auth", null)
                .also {
                    putString("auth", it)

                    log.debug {
                        "apply(): copied_auth"
                    }
                }
        }

        getEncryptedPrefsFile().delete()

        log.debug {
            "apply(): applied_successfully"
        }
    }

    private fun getEncryptedPrefsFile(): File =
        File(
            File(
                context.applicationInfo.dataDir,
                "shared_prefs"
            ),
            "$ENCRYPTED_PREFS_NAME.xml"
        )

    private companion object {
        private const val ENCRYPTED_PREFS_NAME = "auth"
    }
}
