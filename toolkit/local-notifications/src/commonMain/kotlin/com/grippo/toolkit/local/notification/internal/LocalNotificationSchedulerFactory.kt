package com.grippo.toolkit.local.notification.internal

import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.local.notification.LocalNotificationScheduler

internal expect fun NativeContext.getLocalNotificationScheduler(): LocalNotificationScheduler
