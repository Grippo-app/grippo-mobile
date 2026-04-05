package com.grippo.toolkit.local.notification

import kotlin.time.Duration

public interface NotificationManager {

    public fun show(notification: AppNotification, delay: Duration = Duration.ZERO): NotificationKey

    public fun cancel(id: NotificationKey)

    public suspend fun isPending(id: NotificationKey): Boolean
}
