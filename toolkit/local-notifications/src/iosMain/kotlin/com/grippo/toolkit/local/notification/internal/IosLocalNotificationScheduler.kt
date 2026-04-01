package com.grippo.toolkit.local.notification.internal

import com.grippo.toolkit.local.notification.LocalNotification
import com.grippo.toolkit.local.notification.LocalNotificationScheduler
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Key used in `UNNotificationContent.userInfo` to carry the deeplink string. */
private const val USER_INFO_DEEPLINK_KEY = "deeplink"

/**
 * iOS implementation of [LocalNotificationScheduler] backed by
 * [UNUserNotificationCenter].
 *
 * Authorization is requested once on first instantiation. The system will
 * prompt the user with the standard iOS notification-permission dialog if the
 * app has not done so before. Notifications are silently dropped by the OS
 * when the user has denied permission.
 *
 * Pending notification requests do **not** survive a full app reinstall;
 * callers are responsible for re-scheduling after such events.
 */
internal class IosLocalNotificationScheduler : LocalNotificationScheduler {

    private val notificationCenter: UNUserNotificationCenter =
        UNUserNotificationCenter.currentNotificationCenter()

    init {
        requestAuthorization()
    }

    override fun schedule(notification: LocalNotification) {
        val nowMs = (NSDate().timeIntervalSince1970 * 1_000.0).toLong()
        val delaySeconds = (notification.deliverAtEpochMillis - nowMs) / 1_000.0
        // Silently drop past-due notifications.
        if (delaySeconds <= 0.0) return

        // Direct property assignment on the explicitly-typed variable avoids the
        // Kotlin/Native issue where `userInfo` is inferred as `val` from the
        // read-only base class `UNNotificationContent` inside a lambda receiver.
        val content = UNMutableNotificationContent()
        content.setTitle(notification.title)
        content.setBody(notification.body)
        // Store the deeplink in userInfo so the app can read it from
        // UNNotificationResponse.notification.request.content.userInfo
        // inside UNUserNotificationCenterDelegate.didReceive(_:withCompletionHandler:).
        if (notification.deeplink != null) {
            content.setUserInfo(mapOf<Any?, Any?>(USER_INFO_DEEPLINK_KEY to notification.deeplink))
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = delaySeconds,
            repeats = false,
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notification.id.toString(),
            content = content,
            trigger = trigger,
        )

        notificationCenter.addNotificationRequest(request, withCompletionHandler = null)
    }

    override fun cancel(id: Int) {
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(
            listOf(id.toString()),
        )
    }

    /**
     * Queries `UNUserNotificationCenter` for pending requests and checks whether
     * one with [id] exists.  The underlying API is callback-based; [suspendCoroutine]
     * bridges it to a coroutine without any additional library dependency.
     */
    override suspend fun isPending(id: Int): Boolean = suspendCoroutine { continuation ->
        notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val found = requests
                ?.filterIsInstance<UNNotificationRequest>()
                ?.any { it.identifier == id.toString() }
                ?: false
            continuation.resume(found)
        }
    }

    private fun requestAuthorization() {
        val options = UNAuthorizationOptionAlert or
                UNAuthorizationOptionSound or
                UNAuthorizationOptionBadge
        notificationCenter.requestAuthorizationWithOptions(
            options = options,
            completionHandler = { _, _ -> },
        )
    }
}
