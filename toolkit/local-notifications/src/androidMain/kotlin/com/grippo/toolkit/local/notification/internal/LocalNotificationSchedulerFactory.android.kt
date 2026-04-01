package com.grippo.toolkit.local.notification.internal

import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.local.notification.LocalNotificationScheduler

internal actual fun NativeContext.getLocalNotificationScheduler(): LocalNotificationScheduler {
    return AndroidLocalNotificationScheduler(context)
}
