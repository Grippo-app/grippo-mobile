package com.grippo.toolkit.local.notification.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver fired by AlarmManager at the scheduled delivery time.
 * Delegates notification building and posting to [buildAndPostNotification].
 */
internal class ScheduledNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.ensureNotificationChannel()
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_NOTIFICATION_BODY) ?: return
        val deeplink = intent.getStringExtra(EXTRA_NOTIFICATION_DEEPLINK)
        context.buildAndPostNotification(id = id, title = title, body = body, deeplink = deeplink)
    }
}
