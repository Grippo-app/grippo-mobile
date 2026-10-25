package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.synchronization.PushTokenSynchronizer
import com.grippo.toolkit.messaging.token.PushToken
import org.koin.core.scope.Scope

internal class DeferredPushTokenSynchronizer(private val scope: Scope) : PushTokenSynchronizer {
    override suspend fun syncToken(token: PushToken) {
        scope.get<PushTokenSynchronizer>().syncToken(token)
    }
}
