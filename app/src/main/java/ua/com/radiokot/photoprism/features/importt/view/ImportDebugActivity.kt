package ua.com.radiokot.photoprism.features.importt.view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import ua.com.radiokot.photoprism.base.view.BaseActivity

class ImportDebugActivity : BaseActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(TextView(this).apply {
            setPadding(40, 40, 40, 40)
            text =
                """
                    Action: ${intent.action}
                    
                    Type: ${intent.type}
                    
                    Uri: ${intent.data}
                    
                    Stream[]: ${intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)}
                    
                    Stream: ${intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)}
                """.trimIndent()
        })
    }
}
