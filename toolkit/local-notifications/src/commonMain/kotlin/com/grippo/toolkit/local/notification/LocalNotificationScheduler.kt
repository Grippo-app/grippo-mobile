package com.grippo.toolkit.local.notification

public interface LocalNotificationScheduler {

    /**
     * Schedules [notification] for delivery at [LocalNotification.deliverAtEpochMillis].
     *
     * Calling this with the same [LocalNotification.id] as an already-pending
     * notification replaces the previous one.  Notifications whose delivery time
     * is in the past are silently dropped.
     */
    public fun schedule(notification: LocalNotification)

    /**
     * Cancels the pending notification identified by [id].
     * Has no effect if no notification with that [id] is pending.
     */
    public fun cancel(id: Int)

    /**
     * Returns `true` if a notification with [id] is currently queued for future delivery.
     *
     * Useful for avoiding duplicate scheduling — e.g. check before scheduling a daily
     * workout reminder so the same alarm is not registered twice.
     *
     * **Android:** checks for an existing [PendingIntent] via `FLAG_NO_CREATE`.
     * Returns `false` after a device reboot even if [schedule] was called before,
     * because `AlarmManager` alarms do not survive reboots.
     *
     * **iOS:** queries `UNUserNotificationCenter.getPendingNotificationRequests`
     * asynchronously; the suspend bridges the callback to a coroutine.
     */
    public suspend fun isPending(id: Int): Boolean
}
