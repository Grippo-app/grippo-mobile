package com.grippo.toolkit.messaging.transport

import com.grippo.toolkit.messaging.state.PushTokenState

public interface PushTokenProvider {
    public suspend fun fetchToken(): PushTokenState
}
