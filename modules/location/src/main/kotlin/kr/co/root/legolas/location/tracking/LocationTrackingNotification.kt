package kr.co.root.legolas.location.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kr.co.root.legolas.location.R
import javax.inject.Inject

class LocationTrackingNotification @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun build(motion: RootActivityMotion): Notification {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Location timeline", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val stopIntent = PendingIntent.getService(
            context,
            4102,
            Intent(context, LocationTrackingService::class.java)
                .setAction(LocationTrackingService.ActionDisable),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Location timeline is active")
            .setContentText("Current state: ${motion.storageKey}")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.location_stop_collection), stopIntent)
            .build()
    }

    fun update(motion: RootActivityMotion) {
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, build(motion))
    }

    companion object {
        const val NOTIFICATION_ID = 4101
        private const val CHANNEL_ID = "legolas_location_tracking"
    }
}
