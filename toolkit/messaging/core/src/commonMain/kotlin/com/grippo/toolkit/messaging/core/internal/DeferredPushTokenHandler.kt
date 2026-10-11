package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.PushToken
import com.grippo.toolkit.messaging.PushTokenHandler
import org.koin.core.scope.Scope

/** Resolves on callback because authorization depends on Messaging itself. */
internal class DeferredPushTokenHandler(private val scope: Scope) : PushTokenHandler {
    override suspend fun onTokenRefresh(token: PushToken) {
        scope.get<PushTokenHandler>().onTokenRefresh(token)
    }
}
