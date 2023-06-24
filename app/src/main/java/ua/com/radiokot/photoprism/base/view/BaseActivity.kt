package ua.com.radiokot.photoprism.base.view

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import org.koin.android.ext.android.getKoin
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.envconnection.view.EnvConnectionActivity
import ua.com.radiokot.photoprism.util.LocalizationHelper
import java.util.Locale

abstract class BaseActivity : AppCompatActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        // Prefer the session scope, but allow running without it.
        getKoin().getScopeOrNull(DI_SCOPE_SESSION)
            ?.apply { linkTo(createActivityScope()) }
            ?: createActivityScope()
    }

    private val log = kLogger("BaseActivity")

    protected open val hasSession: Boolean
        get() = scope.getOrNull<EnvSession>() != null

    @get:ColorInt
    protected open val windowBackgroundColor: Int by lazy {
        MaterialColors.getColor(
            this,
            android.R.attr.colorBackground,
            Color.RED
        )
    }

    /**
     * @return true if there is no session and switching to the env connection has been called.
     */
    open fun goToEnvConnectionIfNoSession(): Boolean =
        if (!hasSession) {
            log.warn {
                "goToEnvConnectionIfNoSession(): going"
            }

            goToEnvConnection()
            true
        } else {
            false
        }

    /**
     * @return true if there is no session and finishing has been called.
     */
    open fun finishIfNoSession(): Boolean =
        if (!hasSession) {
            log.warn {
                "finishIfNoSession(): finishing"
            }

            finishAffinity()
            true
        } else {
            false
        }

    /**
     * Goes to the [EnvConnectionActivity] finishing this activity and all the underlying.
     */
    protected open fun goToEnvConnection() {
        log.debug {
            "goToEnvConnection(): going_to_env_connection"
        }

        startActivity(Intent(this, EnvConnectionActivity::class.java))
        finishAffinity()
    }

    override fun attachBaseContext(newBase: Context) {
        val stringsLocale = LocalizationHelper.getLocaleOfStrings(newBase.resources)
        Locale.setDefault(stringsLocale)
        super.attachBaseContext(
            LocalizationHelper.getLocalizedConfigurationContext(
                context = newBase,
                locale = stringsLocale,
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Reset the splash background.
        window.setBackgroundDrawable(ColorDrawable(windowBackgroundColor))

        super.onCreate(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
