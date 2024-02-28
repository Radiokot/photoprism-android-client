package ua.com.radiokot.photoprism.features.memories.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.view.GalleryActivity

class MemoriesNotificationsManager(
    private val context: Context,
) {
    private val notificationsManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val canNotify: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    fun notifyNewMemories(): Notification {
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.memories_notification_new_memories_title))
            .setContentText(context.getString(R.string.memories_notification_new_memories_text))
            .setColor(ContextCompat.getColor(context, R.color.md_theme_light_primary))
            // White icon is used for Android 5 compatibility.
            .setSmallIcon(R.drawable.ic_photo_album_white)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, GalleryActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .setAutoCancel(true)
            .build()

        @SuppressLint("MissingPermission")
        if (canNotify) {
            notificationsManager.notify(NEW_MEMORIES_NOTIFICATION_ID, notification)
        }

        return notification
    }

    private fun ensureChannel() {
        notificationsManager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(
                    CHANNEL_ID,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                )
                    .setName(context.getString(R.string.memories_notification_channel_name))
                    .setDescription(context.getString(R.string.memories_notification_channel_description))
                    .build()
            )
        )
    }

    private companion object {
        private const val CHANNEL_ID = "memories"
        private const val NEW_MEMORIES_NOTIFICATION_ID = 332445
    }
}
