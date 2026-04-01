package com.grippo.toolkit.local.notification

/**
 * Represents a local notification to be delivered on the same device at a scheduled time.
 *
 * @property id        Unique identifier. Used to update or cancel a pending notification.
 * @property title     Short headline shown in the notification drawer.
 * @property body      Supporting text shown below the title.
 * @property deliverAtEpochMillis  Absolute delivery time as Unix epoch milliseconds (UTC).
 *                                 Notifications whose delivery time has already passed are dropped.
 * @property deeplink  Optional destination the app should navigate to when the user taps the
 *                     notification. The value is intentionally opaque to this module — it is
 *                     stored and forwarded as-is; routing logic lives in the host app.
 *
 *                     **Android:** delivered as an Intent extra ([Intent.getStringExtra]) to the
 *                     activity returned by [PackageManager.getLaunchIntentForPackage].
 *                     Read it in `Activity.onNewIntent()` or `onCreate()` using the key
 *                     `"grippo_local_notification_deeplink"`.
 *
 *                     **iOS:** stored in `UNNotificationContent.userInfo` under the key
 *                     `"deeplink"`.  Read it in
 *                     `UNUserNotificationCenterDelegate.didReceive(_:withCompletionHandler:)`.
 */
public data class LocalNotification(
    val id: Int,
    val title: String,
    val body: String,
    val deliverAtEpochMillis: Long,
    val deeplink: String? = null,
)
