package com.grippo.toolkit.messaging.synchronization

import com.grippo.toolkit.messaging.token.PushToken

public interface PushTokenSynchronizer {
    public suspend fun syncToken(token: PushToken)
}
