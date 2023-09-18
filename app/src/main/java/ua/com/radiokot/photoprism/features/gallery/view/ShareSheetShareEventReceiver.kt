package ua.com.radiokot.photoprism.features.gallery.view

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.features.gallery.view.ShareSheetShareEventReceiver.Companion.getPendingIntent
import ua.com.radiokot.photoprism.features.gallery.view.ShareSheetShareEventReceiver.Companion.shareEvents
import kotlin.random.Random

/**
 * A [BroadcastReceiver] that allows subscribing to successful share events
 * of the system share sheet ([Intent.createChooser]).
 * An event is emitted whenever an app is open from the sheet.
 *
 * @see getPendingIntent
 * @see shareEvents
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class ShareSheetShareEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        shareEventsSubject.onNext(true)
    }

    companion object {
        private val shareEventsSubject: PublishSubject<Boolean> = PublishSubject.create()

        /**
         * Emits **true** whenever whenever an app is open from the selection sheet.
         */
        val shareEvents: Observable<Boolean> = shareEventsSubject

        /**
         * @return a [PendingIntent] sender of which needs to be passed to [Intent.createChooser]
         */
        fun getPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt() and 0xffff,
                Intent(context, ShareSheetShareEventReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            PendingIntent.FLAG_IMMUTABLE
                        else
                            0,
            )
    }
}
