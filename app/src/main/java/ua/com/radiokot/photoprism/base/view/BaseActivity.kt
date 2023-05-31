package ua.com.radiokot.photoprism.base.view

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import ua.com.radiokot.photoprism.util.LocalizationHelper
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}