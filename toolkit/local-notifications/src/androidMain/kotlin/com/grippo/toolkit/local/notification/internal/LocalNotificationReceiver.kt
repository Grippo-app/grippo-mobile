package com.grippo.toolkit.local.notification.internal

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * BroadcastReceiver fired by the AlarmManager at the scheduled time.
 * Builds and posts the notification using only the Android framework API
 * (minSdk 26, no compat layer required).
 */
internal class LocalNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Ensure the channel exists in case the app was force-stopped and restarted
        context.ensureNotificationChannel()

        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_NOTIFICATION_BODY) ?: return

        // On API 33+ the POST_NOTIFICATIONS permission must have been granted;
        // if not, silently drop the notification rather than crashing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) return
        }

        // Resolve the host app's notification icon (drawable named "ic_notification"),
        // falling back to the system info icon so the library never crashes at runtime.
        val iconRes = context.resources
            .getIdentifier("ic_notification", "drawable", context.packageName)
            .takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info

        val builder = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)

        // If a deeplink was provided, wire up a tap action that re-launches the app
        // and forwards the deeplink as an Intent extra.
        // The host Activity reads it in onNewIntent() / onCreate() via:
        //   intent.getStringExtra("grippo_local_notification_deeplink")
        val deeplink = intent.getStringExtra(EXTRA_NOTIFICATION_DEEPLINK)
        if (deeplink != null) {
            val contentIntent = buildDeeplinkIntent(context, deeplink)
            builder.setContentIntent(contentIntent)
        }

        context.getSystemService(NotificationManager::class.java)
            ?.notify(id, builder.build())
    }

    private fun buildDeeplinkIntent(context: Context, deeplink: String): PendingIntent {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NOTIFICATION_DEEPLINK, deeplink)
            }
            ?: Intent() // fallback: empty intent, app opens at root

        return PendingIntent.getActivity(
            context,
            deeplink.hashCode(), // unique request code per deeplink value
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
