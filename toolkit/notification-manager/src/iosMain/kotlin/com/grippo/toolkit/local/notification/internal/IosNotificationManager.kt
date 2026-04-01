package com.grippo.toolkit.local.notification.internal

import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationManager
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class IosNotificationManager : NotificationManager {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun show(notification: AppNotification) {
        // Trigger with a minimal interval — effectively immediate.
        // UNUserNotificationCenter requires a non-zero interval.
        post(notification, triggerIntervalSeconds = 0.1)
    }

    override fun schedule(notification: AppNotification, deliverAtEpochMillis: Long) {
        val nowMs = (NSDate().timeIntervalSince1970 * 1_000.0).toLong()
        val delaySeconds = (deliverAtEpochMillis - nowMs) / 1_000.0
        if (delaySeconds <= 0.0) return
        post(notification, triggerIntervalSeconds = delaySeconds)
    }

    override fun cancel(id: Int) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id.toString()))
    }

    override suspend fun isPending(id: Int): Boolean = suspendCoroutine { cont ->
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val found = requests
                ?.filterIsInstance<UNNotificationRequest>()
                ?.any { it.identifier == id.toString() }
                ?: false
            cont.resume(found)
        }
    }

    private fun post(notification: AppNotification, triggerIntervalSeconds: Double) {
        val content = UNMutableNotificationContent()
        content.setTitle(notification.title)
        content.setBody(notification.body)
        if (notification.deeplink != null) {
            content.setUserInfo(mapOf<Any?, Any?>("deeplink" to notification.deeplink))
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = triggerIntervalSeconds,
            repeats = false,
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notification.id.toString(),
            content = content,
            trigger = trigger,
        )

        center.addNotificationRequest(request, withCompletionHandler = null)
    }
}
