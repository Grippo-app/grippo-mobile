package com.grippo.toolkit.local.notification

import kotlin.time.Duration

public interface NotificationManager {

    public fun show(notification: AppNotification, delay: Duration = Duration.ZERO): Int

    public fun cancel(id: Int)

    public suspend fun isPending(id: Int): Boolean
}
