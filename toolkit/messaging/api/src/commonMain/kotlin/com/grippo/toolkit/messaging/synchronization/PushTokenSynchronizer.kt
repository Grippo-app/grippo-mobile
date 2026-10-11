package com.grippo.toolkit.messaging.synchronization

import com.grippo.toolkit.messaging.token.PushToken

/** Registers a valid token with the backend; failures must propagate to the core. */
public interface PushTokenSynchronizer {
    public suspend fun syncToken(token: PushToken)
}
