package com.grippo.toolkit.local.notification.internal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.grippo.toolkit.local.notification.LocalNotificationExtras

internal const val EXTRA_NOTIFICATION_DEEPLINK = LocalNotificationExtras.DEEPLINK

internal const val NOTIFICATION_CHANNEL_ID = "grippo_local_notifications"
private const val NOTIFICATION_CHANNEL_NAME = "Grippo Notifications"
private const val NOTIFICATION_CHANNEL_DESC = "Reminders and scheduled messages from Grippo"

internal const val EXTRA_NOTIFICATION_ID = "grippo_local_notification_id"
internal const val EXTRA_NOTIFICATION_TITLE = "grippo_local_notification_title"
internal const val EXTRA_NOTIFICATION_BODY = "grippo_local_notification_body"

/**
 * Creates the notification channel if it does not already exist.
 * Safe to call multiple times — channel creation is idempotent.
 */
internal fun Context.ensureNotificationChannel() {
    val manager = getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return

    val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = NOTIFICATION_CHANNEL_DESC
    }
    manager.createNotificationChannel(channel)
}
