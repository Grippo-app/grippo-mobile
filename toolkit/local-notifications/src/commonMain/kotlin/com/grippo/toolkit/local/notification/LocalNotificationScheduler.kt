package com.grippo.toolkit.local.notification

/**
 * Schedules and cancels device-local notifications without requiring a network round-trip.
 *
 * Platform requirements
 * ---------------------
 * **Android:** The module manifest declares `POST_NOTIFICATIONS` (API 33+) and
 * `SCHEDULE_EXACT_ALARM` (API 31+).  On API 31+ the scheduler checks
 * [AlarmManager.canScheduleExactAlarms] and falls back to an inexact alarm when
 * the user has not granted the exact-alarm permission.
 * The `POST_NOTIFICATIONS` runtime permission must be requested by the host
 * Activity before delivered notifications will appear on API 33+ devices.
 *
 * **iOS:** `UNUserNotificationCenter` authorization is requested once on first
 * use. The user will see the system authorization dialog if it has not been
 * shown before.
 *
 * Persistence note
 * ----------------
 * Pending alarms do **not** survive a device reboot (Android) or a full app
 * reinstall (iOS). Re-schedule any outstanding notifications after the app
 * starts if boot-time persistence is required.
 */
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
