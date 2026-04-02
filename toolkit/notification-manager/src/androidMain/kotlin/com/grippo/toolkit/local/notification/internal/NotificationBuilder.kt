package com.grippo.toolkit.local.notification.internal

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Builds and posts a notification. Shared between [AndroidNotificationManager.show]
 * (immediate) and [ScheduledNotificationReceiver] (scheduled, fired by AlarmManager).
 */
internal fun Context.buildAndPostNotification(
    id: Int,
    title: String,
    body: String,
    deeplink: String?,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
        if (granted != PackageManager.PERMISSION_GRANTED) return
    }

    val iconRes = applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info

    val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(iconRes)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)

    if (deeplink != null) {
        builder.setContentIntent(buildDeeplinkIntent(deeplink))
    }

    getSystemService(NotificationManager::class.java)?.notify(id, builder.build())
}

private fun Context.buildDeeplinkIntent(deeplink: String): PendingIntent {
    val launchIntent = packageManager
        .getLaunchIntentForPackage(packageName)
        ?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_DEEPLINK, deeplink)
        }
        ?: Intent()

    return PendingIntent.getActivity(
        this,
        deeplink.hashCode(),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
