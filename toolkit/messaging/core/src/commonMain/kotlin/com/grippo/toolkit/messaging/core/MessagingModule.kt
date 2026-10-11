package com.grippo.toolkit.messaging.core

import com.grippo.toolkit.messaging.Messaging
import com.grippo.toolkit.messaging.MessagingBootstrap
import com.grippo.toolkit.messaging.PushToken
import com.grippo.toolkit.messaging.PushTokenHandler
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
public class MessagingModule {
    @Single(binds = [Messaging::class, MessagingBootstrap::class])
    internal fun provideMessaging(scope: Scope): DefaultMessaging = DefaultMessaging(
        // Resolve only on callback: authorization depends on Messaging itself.
        handler = object : PushTokenHandler {
            override suspend fun onTokenRefresh(token: PushToken) {
                scope.get<PushTokenHandler>().onTokenRefresh(token)
            }
        },
    )
}
