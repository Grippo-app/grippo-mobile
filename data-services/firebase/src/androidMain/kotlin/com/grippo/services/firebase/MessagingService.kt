package com.grippo.services.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import com.grippo.toolkit.messaging.lifecycle.MessagingBootstrap
import org.koin.core.component.KoinComponent

public class MessagingService : FirebaseMessagingService(), KoinComponent {

    private val messaging: MessagingBootstrap by getKoin().inject()
    private val notificationManager: NotificationManager by getKoin().inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        messaging.onTokenChanged(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        notificationManager.show(
            AppNotification(
                id = NotificationKey.Custom(message.messageId.hashCode()),
                title = message.notification?.title ?: return,
                body = message.notification?.body ?: return,
                deeplink = message.data["deeplink"],
            )
        )
    }
}
