package com.grippo.toolkit.messaging.transport

import com.grippo.toolkit.messaging.state.PushTokenState

/** Platform transport for explicit token retrieval; SDK callbacks use MessagingBootstrap. */
public interface PushTokenProvider {
    public suspend fun fetchToken(): PushTokenState
}
