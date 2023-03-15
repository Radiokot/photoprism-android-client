package ua.com.radiokot.photoprism.base.view

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import ua.com.radiokot.photoprism.util.LocalizationHelper

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocalizationHelper.getLocalizedConfigurationContext(
            context = newBase,
            locale = LocalizationHelper.getLocaleOfStrings(newBase.resources),
        ))
    }
}