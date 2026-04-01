package com.grippo.toolkit.local.notification.internal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.grippo.toolkit.local.notification.LocalNotification
import com.grippo.toolkit.local.notification.LocalNotificationScheduler

/**
 * Android implementation of [LocalNotificationScheduler].
 *
 * Delivery strategy (in priority order):
 * 1. `AlarmManager.setExactAndAllowWhileIdle` when exact alarms are permitted
 *    (always on API < 31; requires user consent via Settings on API 31+).
 * 2. `AlarmManager.setAndAllowWhileIdle` (inexact, OS may delay by minutes) when
 *    the exact-alarm permission has not been granted.
 *
 * Notifications whose [LocalNotification.deliverAtEpochMillis] is in the past
 * are silently dropped to avoid an immediate flood of stale notifications.
 */
internal class AndroidLocalNotificationScheduler(
    private val context: Context,
) : LocalNotificationScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        context.ensureNotificationChannel()
    }

    override fun schedule(notification: LocalNotification) {
        val nowMs = System.currentTimeMillis()
        if (notification.deliverAtEpochMillis <= nowMs) return

        val pendingIntent = buildPendingIntent(
            id = notification.id,
            updateExisting = true,
        ) {
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_NOTIFICATION_TITLE, notification.title)
            putExtra(EXTRA_NOTIFICATION_BODY, notification.body)
            notification.deeplink?.let { putExtra(EXTRA_NOTIFICATION_DEEPLINK, it) }
        }

        scheduleAlarm(notification.deliverAtEpochMillis, pendingIntent)
    }

    override fun cancel(id: Int) {
        val pendingIntent = buildPendingIntent(
            id = id,
            updateExisting = false,
        ) {} ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Checks whether an alarm with this [id] is currently registered in [AlarmManager].
     *
     * Uses `FLAG_NO_CREATE`: [PendingIntent.getBroadcast] returns `null` when no matching
     * intent exists, which means no alarm is pending.
     */
    override suspend fun isPending(id: Int): Boolean {
        val intent = Intent(context, LocalNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        return pendingIntent != null
    }

    // -------------------------------------------------------------------------

    private fun scheduleAlarm(triggerAtMs: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                // Inexact fallback — the OS may batch-deliver this a few minutes late.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
        } else {
            // Below API 31, setExactAndAllowWhileIdle does not require a special permission.
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        }
    }

    /**
     * Returns a [PendingIntent] for [LocalNotificationReceiver].
     *
     * @param updateExisting  When `true`, uses `FLAG_UPDATE_CURRENT` so re-scheduling
     *                        the same [id] replaces the previous extras.
     *                        When `false`, uses `FLAG_NO_CREATE` and returns `null` if
     *                        no matching intent exists (used by [cancel]).
     */
    private fun buildPendingIntent(
        id: Int,
        updateExisting: Boolean,
        extrasBlock: Intent.() -> Unit,
    ): PendingIntent? {
        val intent = Intent(context, LocalNotificationReceiver::class.java).apply(extrasBlock)
        val flags = if (updateExisting) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, id, intent, flags)
    }
}
