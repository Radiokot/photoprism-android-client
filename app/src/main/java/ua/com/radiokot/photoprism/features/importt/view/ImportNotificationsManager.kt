package ua.com.radiokot.photoprism.features.importt.view

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.GalleryActivity
import kotlin.math.roundToInt

class ImportNotificationsManager(
    private val context: Context,
) {
    private val log = kLogger("ImportNotificationsManager")
    private val notificationsManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val areNotificationsEnabled: Boolean
        get() = notificationsManager.areNotificationsEnabled()

    fun getImportProgressNotification(progressPercent: Double): Notification =
        NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setContentTitle(context.getString(R.string.import_notification_progress_title))
            .setProgress(
                100,
                progressPercent.roundToInt().coerceAtLeast(1),
                progressPercent == -1.0
            )
            .setColor(ContextCompat.getColor(context, R.color.md_theme_light_primary))
            // White icon is used for Android 5 compatibility.
            .setSmallIcon(R.drawable.ic_upload_white)
            .setAutoCancel(true)
            .build()
            .also { ensureChannel() }

    fun notifySuccessfulImport(uploadToken: String): Notification {
        ensureChannel()

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setContentTitle(context.getString(R.string.import_notification_success_title))
            .setColor(ContextCompat.getColor(context, R.color.md_theme_light_primary))
            // White icon is used for Android 5 compatibility.
            .setSmallIcon(R.drawable.ic_upload_white)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, GalleryActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()

        @SuppressLint("MissingPermission")
        if (areNotificationsEnabled) {
            notificationsManager.notify(
                getImportResultNotificationId(uploadToken),
                notification
            )
        } else {
            log.debug {
                "notifySuccessfulImport(): skip_notify_as_disabled"
            }
        }

        return notification
    }

    fun notifyFailedImport(uploadToken: String): Notification {
        ensureChannel()

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setContentTitle(context.getString(R.string.import_notification_failed_title))
            .setContentText(context.getString(R.string.import_notification_failed_text))
            .setColor(ContextCompat.getColor(context, R.color.md_theme_light_primary))
            // White icon is used for Android 5 compatibility.
            .setSmallIcon(R.drawable.ic_upload_white)
            .setAutoCancel(true)
            .build()

        @SuppressLint("MissingPermission")
        if (areNotificationsEnabled) {
            notificationsManager.notify(
                getImportResultNotificationId(uploadToken),
                notification
            )
        } else {
            log.debug {
                "notifyFailedImport(): skip_notify_as_disabled"
            }
        }

        return notification
    }

    private fun ensureChannel() {
        notificationsManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName(context.getString(R.string.import_notification_channel_name))
                .setDescription(context.getString(R.string.import_notification_channel_description))
                .build()
        )
    }

    companion object {
        private const val CHANNEL_ID = "import"

        fun getImportProgressNotificationId(uploadToken: String): Int =
            uploadToken.hashCode()

        private fun getImportResultNotificationId(uploadToken: String): Int =
            uploadToken.hashCode() + 13
    }
}
