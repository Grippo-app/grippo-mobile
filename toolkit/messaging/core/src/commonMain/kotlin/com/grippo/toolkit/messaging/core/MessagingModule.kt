package com.grippo.toolkit.messaging.core

import com.grippo.toolkit.messaging.Messaging
import com.grippo.toolkit.messaging.core.internal.DefaultMessaging
import com.grippo.toolkit.messaging.core.internal.DeferredPushTokenSynchronizer
import com.grippo.toolkit.messaging.lifecycle.MessagingBootstrap
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
public class MessagingModule {
    @Single(binds = [Messaging::class, MessagingBootstrap::class])
    internal fun provideMessaging(scope: Scope): DefaultMessaging = DefaultMessaging(
        synchronizer = DeferredPushTokenSynchronizer(scope),
    )
}
