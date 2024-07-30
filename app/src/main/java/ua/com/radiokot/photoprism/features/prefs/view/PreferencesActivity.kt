package ua.com.radiokot.photoprism.features.prefs.view

import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityPreferencesBinding
import ua.com.radiokot.photoprism.extension.checkNotNull

class PreferencesActivity :
    BaseActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var view: ActivityPreferencesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToEnvConnectionIfNoSession()) {
            return
        }

        view = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, PreferencesFragment())
                .disallowAddToBackStack()
                .commit()
        }
    }

    @Suppress("DEPRECATION")
    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        preference: Preference
    ): Boolean {
        val fragment = supportFragmentManager.fragmentFactory
            .instantiate(
                classLoader,
                preference.fragment.checkNotNull {
                    "How come the preference has no fragment if this method is called on it?"
                }
            )
            .apply {
                arguments = preference.extras
                setTargetFragment(caller, 0)
            }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(preference.key)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()

        return true
    }
}
