package ua.com.radiokot.photoprism.features.env.view

import android.os.Bundle
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityEnvConnectionBinding

class EnvConnectionActivity : BaseActivity() {
    private lateinit var view: ActivityEnvConnectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityEnvConnectionBinding.inflate(layoutInflater)
        setContentView(view.root)
    }
}