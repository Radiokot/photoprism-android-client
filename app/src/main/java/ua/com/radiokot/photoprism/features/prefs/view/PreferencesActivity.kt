package ua.com.radiokot.photoprism.features.prefs.view

import android.os.Bundle
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityPreferencesBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION

class PreferencesActivity : BaseActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            linkTo(getScope(DI_SCOPE_SESSION))
        }
    }

    private lateinit var view: ActivityPreferencesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
}