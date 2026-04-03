package com.grippo.toolkit.local.notification.internal

import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

internal class IosNotificationManager : NotificationManager {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun show(notification: AppNotification, delay: Duration): NotificationKey {
        val triggerIntervalSeconds = if (delay == Duration.ZERO) {
            // UNUserNotificationCenter requires a non-zero interval — use minimal value.
            0.1
        } else {
            delay.inWholeMilliseconds / 1_000.0
        }
        post(notification, triggerIntervalSeconds = triggerIntervalSeconds)
        return notification.id
    }

    override fun cancel(id: NotificationKey) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id.toString()))
    }

    override suspend fun isPending(id: NotificationKey): Boolean = suspendCoroutine { cont ->
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
