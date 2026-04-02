package com.grippo.toolkit.local.notification.internal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationManager
import kotlin.time.Duration

internal class AndroidNotificationManager(private val context: Context) : NotificationManager {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        context.ensureNotificationChannel()
    }

    override fun show(notification: AppNotification, delay: Duration): Int {
        if (delay == Duration.ZERO) {
            context.buildAndPostNotification(
                id = notification.id,
                title = notification.title,
                body = notification.body,
                deeplink = notification.deeplink,
            )
        } else {
            val deliverAtEpochMillis = System.currentTimeMillis() + delay.inWholeMilliseconds
            val pendingIntent = buildAlarmIntent(id = notification.id, updateExisting = true) {
                putExtra(EXTRA_NOTIFICATION_ID, notification.id)
                putExtra(EXTRA_NOTIFICATION_TITLE, notification.title)
                putExtra(EXTRA_NOTIFICATION_BODY, notification.body)
                notification.deeplink?.let { putExtra(EXTRA_NOTIFICATION_DEEPLINK, it) }
            } ?: return notification.id
            scheduleAlarm(deliverAtEpochMillis, pendingIntent)
        }
        return notification.id
    }

    override fun cancel(id: Int) {
        val pendingIntent = buildAlarmIntent(id = id, updateExisting = false) {} ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override suspend fun isPending(id: Int): Boolean {
        val intent = Intent(context, ScheduledNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        return pendingIntent != null
    }

    private fun scheduleAlarm(triggerAtMs: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    private fun buildAlarmIntent(
        id: Int,
        updateExisting: Boolean,
        extrasBlock: Intent.() -> Unit,
    ): PendingIntent? {
        val intent = Intent(context, ScheduledNotificationReceiver::class.java).apply(extrasBlock)
        val flags = if (updateExisting) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, id, intent, flags)
    }
}
