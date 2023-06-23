package ua.com.radiokot.photoprism.base.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import ua.com.radiokot.photoprism.util.LocalizationHelper
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {
    @get:ColorInt
    protected open val windowBackgroundColor: Int by lazy {
        MaterialColors.getColor(
            this,
            android.R.attr.colorBackground,
            Color.RED
        )
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
