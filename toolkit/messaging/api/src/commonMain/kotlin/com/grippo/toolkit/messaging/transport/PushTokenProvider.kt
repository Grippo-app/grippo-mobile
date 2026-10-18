package com.grippo.toolkit.messaging.transport

import com.grippo.toolkit.messaging.state.PushTokenState

/** Explicit token retrieval; errors throw, while SDK token callbacks use MessagingBootstrap. */
public interface PushTokenProvider {
    public suspend fun fetchToken(): PushTokenState
}
