package com.grippo.toolkit.local.notification

public interface NotificationManager {

    /** Post [notification] immediately. */
    public fun show(notification: AppNotification)

    /**
     * Schedule [notification] for delivery at [deliverAtEpochMillis] (Unix epoch ms, UTC).
     * Calling this with the same [AppNotification.id] replaces any existing pending notification.
     * Past-due notifications are silently dropped.
     */
    public fun schedule(notification: AppNotification, deliverAtEpochMillis: Long)

    /**
     * Cancel the pending scheduled notification identified by [id].
     * Has no effect if no notification with that [id] is pending.
     */
    public fun cancel(id: Int)

    /**
     * Returns `true` if a scheduled notification with [id] is queued for future delivery.
     *
     * **Android:** checks AlarmManager via `FLAG_NO_CREATE`. Returns `false` after reboot.
     * **iOS:** queries `UNUserNotificationCenter.getPendingNotificationRequests`.
     */
    public suspend fun isPending(id: Int): Boolean
}
