package com.grippo.toolkit.local.notification.internal

import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.local.notification.NotificationManager

internal actual fun NativeContext.getNotificationManager(): NotificationManager =
    AndroidNotificationManager(context)
