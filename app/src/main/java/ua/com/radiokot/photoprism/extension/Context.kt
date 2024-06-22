package ua.com.radiokot.photoprism.extension

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Explicitly enables or disables the given app component
 * such as a service or receiver without killing the app.
 *
 * @param componentClass the class of the desired component
 * @param isEnabled whether to enable or disable the component
 *
 * @see PackageManager.setComponentEnabledSetting
 * @see PackageManager.COMPONENT_ENABLED_STATE_ENABLED
 * @see PackageManager.COMPONENT_ENABLED_STATE_DISABLED
 */
fun Context.setManifestComponentEnabled(
    componentClass: Class<*>,
    isEnabled: Boolean,
) {
    packageManager.setComponentEnabledSetting(
        ComponentName(this, componentClass),
        if (isEnabled)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
    )
}

fun Context.isSelfPermissionGranted(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
