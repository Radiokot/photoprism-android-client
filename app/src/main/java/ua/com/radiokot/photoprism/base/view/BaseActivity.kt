package ua.com.radiokot.photoprism.base.view

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import ua.com.radiokot.photoprism.util.LocalizationHelper

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocalizationHelper.getLocalizedConfigurationContext(
                context = newBase,
                locale = LocalizationHelper.getLocaleOfStrings(newBase.resources),
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