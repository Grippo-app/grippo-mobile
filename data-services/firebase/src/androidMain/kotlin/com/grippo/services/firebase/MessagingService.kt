package com.grippo.services.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

public class MessagingService : FirebaseMessagingService(), KoinComponent {

    private val handler: MessagingEventHandler by getKoin().inject()
    private val notificationManager: NotificationManager by getKoin().inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch { handler.onTokenRefresh(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        notificationManager.show(
            AppNotification(
                id = message.messageId.hashCode(),
                title = message.notification?.title ?: return,
                body = message.notification?.body ?: return,
                deeplink = message.data["deeplink"],
            )
        )
    }
}
