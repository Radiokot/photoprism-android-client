package ua.com.radiokot.photoprism.features.ext.key.logic

import android.os.Build
import com.google.common.hash.Hashing

object AndroidHardwareIdentifier : HardwareIdentifier {
    private val identifier: String by lazy {
        StringBuilder()
            .append(Build.BOARD)
            .append(Build.BRAND)
            .append(Build.DEVICE)
            .append(Build.HARDWARE)
            .append(Build.MANUFACTURER)
            .append(Build.MODEL)
            .append(Build.PRODUCT)
            .append(Build.SUPPORTED_ABIS.sorted().joinToString())
            .toString()
            .let { Hashing.sha256().hashString(it, Charsets.UTF_8) }
            .toString()
    }

    override fun getHardwareIdentifier(): String =
        identifier
}
