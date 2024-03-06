package ua.com.radiokot.photoprism.features.memories.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.GalleryActivity

class MemoriesNotificationsManager(
    private val context: Context,
    private val picasso: Picasso?,
) {
    private val log = kLogger("MemoriesNotificationManager")
    private val notificationsManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val canNotify: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

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
                    "notifyNewMemories(): notifying_with_picture"
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

    fun notifyNewMemories(bigPicture: Bitmap? = null): Notification {
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
        if (canNotify) {
            notificationsManager.notify(NEW_MEMORIES_NOTIFICATION_ID, notification)
        }

        return notification
    }

    fun cancelNewMemoriesNotification() {
        notificationsManager.cancel(NEW_MEMORIES_NOTIFICATION_ID)
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