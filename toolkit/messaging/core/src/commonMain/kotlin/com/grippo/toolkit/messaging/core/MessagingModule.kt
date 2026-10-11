package com.grippo.toolkit.messaging.core

import com.grippo.toolkit.messaging.Messaging
import com.grippo.toolkit.messaging.MessagingBootstrap
import com.grippo.toolkit.messaging.core.internal.DefaultMessaging
import com.grippo.toolkit.messaging.core.internal.DeferredPushTokenHandler
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
public class MessagingModule {
    @Single(binds = [Messaging::class, MessagingBootstrap::class])
    internal fun provideMessaging(scope: Scope): DefaultMessaging = DefaultMessaging(
        handler = DeferredPushTokenHandler(scope),
    )
}
