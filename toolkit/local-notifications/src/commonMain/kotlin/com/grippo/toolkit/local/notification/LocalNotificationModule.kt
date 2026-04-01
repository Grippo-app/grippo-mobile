package com.grippo.toolkit.local.notification

import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.local.notification.internal.getLocalNotificationScheduler
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
@ComponentScan
public class LocalNotificationModule {

    @Single
    internal fun provideLocalNotificationScheduler(nativeContext: NativeContext): LocalNotificationScheduler {
        return nativeContext.getLocalNotificationScheduler()
    }
}
