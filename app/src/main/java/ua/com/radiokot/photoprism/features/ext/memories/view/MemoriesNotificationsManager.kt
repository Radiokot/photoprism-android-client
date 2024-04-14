package ua.com.radiokot.photoprism.features.ext.memories.view

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesPreferences
import ua.com.radiokot.photoprism.features.gallery.view.GalleryActivity


class MemoriesNotificationsManager(
    private val context: Context,
    private val picasso: Picasso?,
    private val memoriesPreferences: MemoriesPreferences,
) {
    private val log = kLogger("MemoriesNotificationManager")
    private val notificationsManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    val areNotificationsEnabled: Boolean
        get() = notificationsManager.areNotificationsEnabled()

    val areMemoriesNotificationsEnabled: Boolean
        get() = areNotificationsEnabled &&
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    memoriesPreferences.areNotificationsEnabled
                } else {
                    ensureChannel()
                    notificationsManager.getNotificationChannelCompat(CHANNEL_ID)
                        ?.let { it.importance > NotificationManagerCompat.IMPORTANCE_NONE }
                        ?: false
                }

    /**
     * Loads the picture and shows a notification with it.
     * If the loading fails, the notification is shown anyway, without the picture.
     */
    fun notifyNewMemories(bigPictureUrl: String): Single<Notification> {
        if (picasso == null) {
            log.debug {
                "notifyNewMemories(): notifying_without_picture_because_no_picasso"
            }

            return Single.just(
                notifyNewMemories(
                    bigPicture = null,
                )
            )
        }

        return picasso
            .load(bigPictureUrl)
            .intoSingle()
            .map { bigPictureBitmap ->
                log.debug {
                    "notifyNewMemories(): notifying_with_picture:" +
                            "\nbigPictureUrl=$bigPictureUrl"
                }

                notifyNewMemories(
                    bigPicture = bigPictureBitmap,
                )
            }
            .onErrorReturn { error ->
                log.debug(error) {
                    "notifyNewMemories(): notifying_without_picture_because_of_error"
                }

                // If the picture can't be loaded, show a notification without it.
                notifyNewMemories(
                    bigPicture = null,
                )
            }
    }

    private fun notifyNewMemories(bigPicture: Bitmap? = null): Notification {
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
            .apply {
                if (bigPicture != null) {
                    setLargeIcon(bigPicture)
                    setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bigPicture)
                            .bigLargeIcon(null)
                    )
                }
            }
            .build()

        @SuppressLint("MissingPermission")
        if (areMemoriesNotificationsEnabled) {
            notificationsManager.notify(NEW_MEMORIES_NOTIFICATION_ID, notification)
        } else {
            log.debug {
                "notifyNewMemories(): skip_notify_as_disabled"
            }
        }

        return notification
    }

    fun cancelNewMemoriesNotification() {
        notificationsManager.cancel(NEW_MEMORIES_NOTIFICATION_ID)
    }

    /**
     * @return an [Intent] to open the relevant system settings page
     * to customize memories notifications:
     * - if notifications are disabled – the notifications settings;
     * - if notifications are enabled – the channel settings.
     */
    fun getSystemSettingsIntent(): Intent {
        ensureChannel()

        return if (areNotificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            }
        else if (!areNotificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        else
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
    }

    private fun ensureChannel() {
        notificationsManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName(context.getString(R.string.memories_notification_channel_name))
                .setDescription(context.getString(R.string.memories_notification_channel_description))
                .setLightColor(ContextCompat.getColor(context, R.color.md_theme_light_primary))
                .build()
        )
    }

    private companion object {
        private const val CHANNEL_ID = "memories"
        private const val NEW_MEMORIES_NOTIFICATION_ID = 332445
    }
}
